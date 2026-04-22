package com.example.tracking_order.modules.order.service;

import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.order.dto.OrderDetailDto;
import com.example.tracking_order.modules.order.dto.OrderListDto;
import com.example.tracking_order.modules.order.dto.OrderStatusUpdateRequest;
import com.example.tracking_order.modules.order.dto.ReturnRequestDto;
import com.example.tracking_order.modules.order.enums.OrderStatus;

import java.time.LocalDateTime;

public interface OrderService {
    PageData<OrderListDto> getOrders(Long userId, OrderStatus status, LocalDateTime fromDate, LocalDateTime toDate, int page, int size);
    OrderDetailDto getOrderDetail(Long orderId);
    OrderDetailDto updateOrderStatus(Long orderId, OrderStatusUpdateRequest request);
    OrderDetailDto requestReturn(Long orderId, ReturnRequestDto request);
}
