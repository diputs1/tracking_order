package com.example.tracking_order.modules.order.controller;

import com.example.tracking_order.common.response.ApiResponse;
import com.example.tracking_order.modules.order.dto.OrderDetailDto;
import com.example.tracking_order.modules.order.dto.OrderStatusUpdateRequest;
import com.example.tracking_order.modules.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shipping/orders")
@RequiredArgsConstructor
public class ShipperController {

    private final OrderService orderService;

    @PatchMapping("/{id}/status")
    public ApiResponse<OrderDetailDto> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        // Here we could add logic specific to shipper app if needed
        return ApiResponse.success(orderService.updateOrderStatus(id, request));
    }
}
