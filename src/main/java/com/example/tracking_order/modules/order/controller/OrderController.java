package com.example.tracking_order.modules.order.controller;

import com.example.tracking_order.common.response.ApiResponse;
import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.order.dto.OrderDetailDto;
import com.example.tracking_order.modules.order.dto.OrderListDto;
import com.example.tracking_order.modules.order.dto.OrderSearchRequest;
import com.example.tracking_order.modules.order.dto.OrderStatusUpdateRequest;
import com.example.tracking_order.modules.order.dto.ReturnRequestDto;
import com.example.tracking_order.modules.order.service.OrderService;
import com.example.tracking_order.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageData<OrderListDto>> getMyOrders(OrderSearchRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        request.setUserId(userDetails.getId());
        return ApiResponse.success(orderService.getOrders(request));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageData<OrderListDto>> getAllOrders(OrderSearchRequest request) {
        // Here admin sees all orders. userId remains null in request.
        return ApiResponse.success(orderService.getOrders(request));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailDto> getOrderDetail(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrderDetail(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderDetailDto> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ApiResponse.success(orderService.updateOrderStatus(orderId, request));
    }

    @PostMapping("/{orderId}/return")
    public ApiResponse<OrderDetailDto> requestReturn(
            @PathVariable Long orderId,
            @Valid @RequestBody ReturnRequestDto request) {
        return ApiResponse.success(orderService.requestReturn(orderId, request));
    }
}
