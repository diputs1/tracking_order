package com.example.tracking_order.modules.shipping.mapper;

import com.example.tracking_order.modules.shipping.dto.ShippingCarrierDto;
import com.example.tracking_order.modules.shipping.entity.ShippingCarrier;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShippingMapper {

    ShippingCarrierDto toShippingCarrierDto(ShippingCarrier shippingCarrier);
}
