package com.example.tracking_order.modules.order.controller;

import com.example.tracking_order.common.response.ApiResponse;
import com.example.tracking_order.modules.order.dto.CreateTrackingLogRequest;
import com.example.tracking_order.modules.order.dto.TrackingLogDto;
import com.example.tracking_order.modules.order.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @GetMapping
    public ApiResponse<List<TrackingLogDto>> getTrackingLogs(@PathVariable Long orderId) {
        return ApiResponse.success(trackingService.getTrackingLogs(orderId));
    }

    @PostMapping
    public ApiResponse<TrackingLogDto> addTrackingLog(
            @PathVariable Long orderId,
            @Valid @RequestBody CreateTrackingLogRequest request) {
        return ApiResponse.success(trackingService.addTrackingLog(orderId, request));
    }
}
