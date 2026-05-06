package com.example.tracking_order.modules.order.service;

import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.order.dto.OrderDetailDto;
import com.example.tracking_order.modules.order.dto.OrderListDto;
import com.example.tracking_order.modules.order.dto.OrderSearchRequest;
import com.example.tracking_order.modules.order.dto.OrderStatusUpdateRequest;
import com.example.tracking_order.modules.order.dto.ReturnRequestDto;


public interface OrderService {
    PageData<OrderListDto> getOrders(OrderSearchRequest request);
    OrderDetailDto getOrderDetail(Long orderId);
    OrderDetailDto updateOrderStatus(Long orderId, OrderStatusUpdateRequest request);
    OrderDetailDto requestReturn(Long orderId, ReturnRequestDto request);
}
