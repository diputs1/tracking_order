package com.example.tracking_order.modules.order.service.impl;

import com.example.tracking_order.common.annotation.LogExecutionTime;
import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.order.dto.OrderDetailDto;
import com.example.tracking_order.modules.order.dto.OrderListDto;
import com.example.tracking_order.modules.order.dto.OrderStatusUpdateRequest;
import com.example.tracking_order.modules.order.dto.ReturnRequestDto;
import com.example.tracking_order.modules.order.entity.Order;
import com.example.tracking_order.modules.order.entity.OrderItem;
import com.example.tracking_order.modules.order.enums.OrderStatus;
import com.example.tracking_order.modules.order.repository.OrderRepository;
import com.example.tracking_order.modules.order.service.OrderService;
import com.example.tracking_order.modules.notification.service.NotificationService;
import com.example.tracking_order.modules.notification.enums.NotificationType;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @LogExecutionTime
    public PageData<OrderListDto> getOrders(Long userId, OrderStatus status, LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {
        Specification<Order> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        List<OrderListDto> items = orderPage.getContent().stream().map(order -> OrderListDto.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .grandTotal(order.getGrandTotal())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                .itemCount(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum())
                .createdAt(order.getCreatedAt())
                .build()).collect(Collectors.toList());

        PageData.Pagination pagination = PageData.Pagination.builder()
                .page(page)
                .totalPages(orderPage.getTotalPages())
                .totalItems(orderPage.getTotalElements())
                .build();

        return PageData.<OrderListDto>builder().items(items).pagination(pagination).build();
    }

    @Override
    public OrderDetailDto getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Đơn hàng không tồn tại"));
        
        List<OrderDetailDto.OrderItemDto> items = order.getItems().stream().map(item -> OrderDetailDto.OrderItemDto.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build()).collect(Collectors.toList());

        OrderDetailDto.ShippingInfo shippingInfo = OrderDetailDto.ShippingInfo.builder()
                .recipientName(order.getReceiverName())
                .recipientPhone(order.getReceiverPhone())
                .address(order.getStreet() + ", " + order.getWard() + ", " + order.getDistrict() + ", " + order.getProvince())
                .carrierName(order.getCarrier() != null ? order.getCarrier().getName() : null)
                .trackingNumber(order.getTrackingNumber())
                .trackingUrl(order.getTrackingUrl())
                .build();

        return OrderDetailDto.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getGrandTotal())
                .shipping(shippingInfo)
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    @LogExecutionTime
    public OrderDetailDto updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Đơn hàng không tồn tại"));
        
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

        return getOrderDetail(orderId);
    }

    @Override
    @Transactional
    public OrderDetailDto requestReturn(Long orderId, ReturnRequestDto request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Đơn hàng không tồn tại"));

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

        return getOrderDetail(orderId);
    }
}
