package com.example.tracking_order.modules.order.controller;

import com.example.tracking_order.common.response.ApiResponse;
import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.order.dto.OrderDetailDto;
import com.example.tracking_order.modules.order.dto.OrderListDto;
import com.example.tracking_order.modules.order.dto.OrderStatusUpdateRequest;
import com.example.tracking_order.modules.order.dto.ReturnRequestDto;
import com.example.tracking_order.modules.order.enums.OrderStatus;
import com.example.tracking_order.modules.order.service.OrderService;
import com.example.tracking_order.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageData<OrderListDto>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from_date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to_date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success(orderService.getOrders(userDetails.getId(), status, from_date, to_date, page, size));
    }

    @GetMapping("/admin")
    public ApiResponse<PageData<OrderListDto>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from_date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to_date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Here admin sees all orders. userId is null.
        return ApiResponse.success(orderService.getOrders(null, status, from_date, to_date, page, size));
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
