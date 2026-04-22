package com.example.tracking_order.modules.shipping.service;

import com.example.tracking_order.modules.shipping.dto.ShippingCarrierDto;
import com.example.tracking_order.modules.shipping.dto.ShippingCarrierRequest;

import java.util.List;

public interface ShippingCarrierService {
    List<ShippingCarrierDto> getAllCarriers(Boolean activeOnly);
    ShippingCarrierDto createCarrier(ShippingCarrierRequest request);
    ShippingCarrierDto updateCarrier(Long id, ShippingCarrierRequest request);
}
