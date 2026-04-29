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
                .order_code(order.getOrderCode())
                .grand_total(order.getGrandTotal())
                .status(order.getStatus().name())
                .payment_status(order.getPaymentStatus().name())
                .payment_method(order.getPaymentMethod().name())
                .item_count(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum())
                .created_at(order.getCreatedAt())
                .build()).collect(Collectors.toList());

        PageData.Pagination pagination = PageData.Pagination.builder()
                .page(page)
                .total_pages(orderPage.getTotalPages())
                .total_items(orderPage.getTotalElements())
                .build();

        return PageData.<OrderListDto>builder().items(items).pagination(pagination).build();
    }

    @Override
    public OrderDetailDto getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Đơn hàng không tồn tại"));
        
        List<OrderDetailDto.OrderItemDto> items = order.getItems().stream().map(item -> OrderDetailDto.OrderItemDto.builder()
                .product_id(item.getProduct().getId())
                .product_name(item.getProductName())
                .product_sku(item.getProductSku())
                .unit_price(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build()).collect(Collectors.toList());

        OrderDetailDto.ShippingInfo shippingInfo = OrderDetailDto.ShippingInfo.builder()
                .recipient_name(order.getReceiverName())
                .recipient_phone(order.getReceiverPhone())
                .address(order.getStreet() + ", " + order.getWard() + ", " + order.getDistrict() + ", " + order.getProvince())
                .carrier_name(order.getCarrier() != null ? order.getCarrier().getName() : null)
                .tracking_number(order.getTrackingNumber())
                .tracking_url(order.getTrackingUrl())
                .build();

        return OrderDetailDto.builder()
                .id(order.getId())
                .order_code(order.getOrderCode())
                .status(order.getStatus().name())
                .payment_status(order.getPaymentStatus().name())
                .payment_method(order.getPaymentMethod().name())
                .subtotal(order.getSubtotal())
                .discount_amount(order.getDiscountAmount())
                .shipping_fee(order.getShippingFee())
                .grand_total(order.getGrandTotal())
                .shipping(shippingInfo)
                .items(items)
                .created_at(order.getCreatedAt())
                .updated_at(order.getUpdatedAt())
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
