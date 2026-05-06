package com.example.tracking_order.modules.order.service.impl;

import com.example.tracking_order.common.annotation.LogExecutionTime;
import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.order.dto.*;
import com.example.tracking_order.modules.order.entity.Order;
import com.example.tracking_order.modules.order.enums.OrderStatus;
import com.example.tracking_order.modules.order.repository.OrderRepository;
import com.example.tracking_order.modules.order.service.OrderService;
import com.example.tracking_order.modules.order.specification.OrderSpecification;
import com.example.tracking_order.modules.notification.service.NotificationService;
import com.example.tracking_order.modules.notification.enums.NotificationType;
import com.example.tracking_order.modules.user.entity.User;
import com.example.tracking_order.modules.user.repository.UserRepository;
import com.example.tracking_order.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final com.example.tracking_order.modules.order.mapper.OrderMapper orderMapper;

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Người dùng không tồn tại"));
    }

    private Order getOrderWithOwnerCheck(Long orderId, User user) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isShipper = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SHIPPER"));
        
        if (isAdmin || isShipper) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Đơn hàng không tồn tại"));
        } else {
            return orderRepository.findByIdAndUserId(orderId, user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập đơn hàng này hoặc đơn hàng không tồn tại"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    @LogExecutionTime
    @Cacheable(value = "orders_list", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '_' + #request.page + '_' + #request.size + '_' + (#request.status ?: 'all') + '_' + (#request.orderCode ?: '')")
    public PageData<OrderListDto> getOrders(OrderSearchRequest request) {
        Specification<Order> spec = OrderSpecification.filterOrders(request);
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), Sort.by("createdAt").descending());
        
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        List<OrderListDto> items = orderPage.getContent().stream()
                .map(orderMapper::toOrderListDto)
                .collect(Collectors.toList());

        PageData.Pagination pagination = PageData.Pagination.builder()
                .page(request.getPage())
                .totalPages(orderPage.getTotalPages())
                .totalItems(orderPage.getTotalElements())
                .build();

        return PageData.<OrderListDto>builder().items(items).pagination(pagination).build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "order_details", key = "#orderId")
    public OrderDetailDto getOrderDetail(Long orderId) {
        User user = getCurrentUser();
        Order order = getOrderWithOwnerCheck(orderId, user);
        return orderMapper.toOrderDetailDto(order);
    }

    @Override
    @Transactional
    @LogExecutionTime
    @CacheEvict(value = "order_details", key = "#orderId")
    public OrderDetailDto updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        User user = getCurrentUser();
        Order order = getOrderWithOwnerCheck(orderId, user);
        
        order.setStatus(request.getStatus());
        
        if (request.getStatus() == OrderStatus.CANCELLED) {
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason(request.getNote());
        } else if (request.getStatus() == OrderStatus.SHIPPING) {
            order.setShippedAt(LocalDateTime.now());
        } else if (request.getStatus() == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        
        orderRepository.save(order);

        notificationService.sendNotification(
            order.getUser(),
            "Cập nhật trạng thái đơn hàng",
            "Đơn hàng " + order.getOrderCode() + " đã được cập nhật sang trạng thái: " + order.getStatus().name(),
            NotificationType.ORDER_STATUS,
            "ORDER",
            order.getId()
        );

        return orderMapper.toOrderDetailDto(order);
    }

    @Override
    @Transactional
    @CacheEvict(value = "order_details", key = "#orderId")
    public OrderDetailDto requestReturn(Long orderId, ReturnRequestDto request) {
        User user = getCurrentUser();
        Order order = getOrderWithOwnerCheck(orderId, user);

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Chỉ có thể yêu cầu trả hàng khi đơn đã giao thành công");
        }

        order.setStatus(OrderStatus.RETURNING);
        order.setReturnReason(request.getReason());
        
        orderRepository.save(order);

        notificationService.sendNotification(
            order.getUser(),
            "Yêu cầu trả hàng",
            "Bạn đã gửi yêu cầu trả hàng cho đơn hàng " + order.getOrderCode(),
            NotificationType.ORDER_STATUS,
            "ORDER",
            order.getId()
        );

        return orderMapper.toOrderDetailDto(order);
    }
}
