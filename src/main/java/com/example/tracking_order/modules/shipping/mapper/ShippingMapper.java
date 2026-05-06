package com.example.tracking_order.modules.shipping.mapper;

import com.example.tracking_order.modules.shipping.dto.ShippingCarrierDto;
import com.example.tracking_order.modules.shipping.entity.ShippingCarrier;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShippingMapper {

    ShippingCarrierDto toShippingCarrierDto(ShippingCarrier shippingCarrier);

    List<ShippingCarrierDto> toShippingCarrierDtoList(List<ShippingCarrier> carriers);
}
