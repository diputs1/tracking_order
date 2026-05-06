package com.example.tracking_order.modules.shipping.controller;

import com.example.tracking_order.common.response.ApiResponse;
import com.example.tracking_order.modules.shipping.dto.ShippingCarrierDto;
import com.example.tracking_order.modules.shipping.dto.ShippingCarrierRequest;
import com.example.tracking_order.modules.shipping.service.ShippingCarrierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping/carriers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ShippingCarrierController {

    private final ShippingCarrierService carrierService;

    @GetMapping
    public ApiResponse<List<ShippingCarrierDto>> getAllCarriers(
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        return ApiResponse.success(carrierService.getAllCarriers(activeOnly));
    }

    @PostMapping
    public ApiResponse<ShippingCarrierDto> createCarrier(@Valid @RequestBody ShippingCarrierRequest request) {
        return ApiResponse.success(carrierService.createCarrier(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ShippingCarrierDto> updateCarrier(
            @PathVariable Long id,
            @RequestBody ShippingCarrierRequest request) {
        return ApiResponse.success(carrierService.updateCarrier(id, request));
    }
}
