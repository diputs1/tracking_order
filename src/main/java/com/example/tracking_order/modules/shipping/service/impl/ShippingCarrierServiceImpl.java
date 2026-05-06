package com.example.tracking_order.modules.shipping.service.impl;

import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.modules.shipping.dto.ShippingCarrierDto;
import com.example.tracking_order.modules.shipping.dto.ShippingCarrierRequest;
import com.example.tracking_order.modules.shipping.entity.ShippingCarrier;
import com.example.tracking_order.modules.shipping.repository.ShippingCarrierRepository;
import com.example.tracking_order.modules.shipping.service.ShippingCarrierService;
import com.example.tracking_order.modules.shipping.mapper.ShippingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingCarrierServiceImpl implements ShippingCarrierService {

    private final ShippingCarrierRepository carrierRepository;
    private final ShippingMapper shippingMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ShippingCarrierDto> getAllCarriers(Boolean activeOnly) {
        List<ShippingCarrier> carriers = (activeOnly != null && activeOnly) 
            ? carrierRepository.findByIsActiveTrue() 
            : carrierRepository.findAll();
            
        return shippingMapper.toShippingCarrierDtoList(carriers);
    }

    @Override
    @Transactional
    public ShippingCarrierDto createCarrier(ShippingCarrierRequest request) {
        ShippingCarrier carrier = ShippingCarrier.builder()
                .name(request.getName())
                .code(request.getCode())
                .trackingUrlTemplate(request.getTrackingUrlTemplate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        carrier = carrierRepository.save(carrier);
        return shippingMapper.toShippingCarrierDto(carrier);
    }

    @Override
    @Transactional
    public ShippingCarrierDto updateCarrier(Long id, ShippingCarrierRequest request) {
        ShippingCarrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Đơn vị vận chuyển không tồn tại"));

        if (request.getName() != null) carrier.setName(request.getName());
        if (request.getCode() != null) carrier.setCode(request.getCode());
        if (request.getTrackingUrlTemplate() != null) carrier.setTrackingUrlTemplate(request.getTrackingUrlTemplate());
        if (request.getIsActive() != null) carrier.setIsActive(request.getIsActive());

        carrierRepository.save(carrier);
        return shippingMapper.toShippingCarrierDto(carrier);
    }

}
