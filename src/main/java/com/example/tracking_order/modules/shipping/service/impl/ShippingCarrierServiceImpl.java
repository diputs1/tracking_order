package com.example.tracking_order.modules.shipping.service.impl;

import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.modules.shipping.dto.ShippingCarrierDto;
import com.example.tracking_order.modules.shipping.dto.ShippingCarrierRequest;
import com.example.tracking_order.modules.shipping.entity.ShippingCarrier;
import com.example.tracking_order.modules.shipping.repository.ShippingCarrierRepository;
import com.example.tracking_order.modules.shipping.service.ShippingCarrierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShippingCarrierServiceImpl implements ShippingCarrierService {

    private final ShippingCarrierRepository carrierRepository;

    @Override
    public List<ShippingCarrierDto> getAllCarriers(Boolean activeOnly) {
        List<ShippingCarrier> carriers = (activeOnly != null && activeOnly) 
            ? carrierRepository.findByIsActiveTrue() 
            : carrierRepository.findAll();
            
        return carriers.stream().map(c -> ShippingCarrierDto.builder()
                .id(c.getId())
                .name(c.getName())
                .code(c.getCode())
                .trackingUrlTemplate(c.getTrackingUrlTemplate())
                .isActive(c.getIsActive())
                .build()).collect(Collectors.toList());
    }

    @Override
    public ShippingCarrierDto createCarrier(ShippingCarrierRequest request) {
        ShippingCarrier carrier = ShippingCarrier.builder()
                .name(request.getName())
                .code(request.getCode())
                .trackingUrlTemplate(request.getTrackingUrlTemplate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        carrier = carrierRepository.save(carrier);
        return mapToDto(carrier);
    }

    @Override
    public ShippingCarrierDto updateCarrier(Long id, ShippingCarrierRequest request) {
        ShippingCarrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Đơn vị vận chuyển không tồn tại"));

        if (request.getName() != null) carrier.setName(request.getName());
        if (request.getCode() != null) carrier.setCode(request.getCode());
        if (request.getTrackingUrlTemplate() != null) carrier.setTrackingUrlTemplate(request.getTrackingUrlTemplate());
        if (request.getIsActive() != null) carrier.setIsActive(request.getIsActive());

        carrierRepository.save(carrier);
        return mapToDto(carrier);
    }

    private ShippingCarrierDto mapToDto(ShippingCarrier carrier) {
        return ShippingCarrierDto.builder()
                .id(carrier.getId())
                .name(carrier.getName())
                .code(carrier.getCode())
                .trackingUrlTemplate(carrier.getTrackingUrlTemplate())
                .isActive(carrier.getIsActive())
                .build();
    }
}
