package com.example.tracking_order.modules.shipping.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingCarrierDto {
    private Long id;
    private String name;
    private String code;
    private String trackingUrlTemplate;
    private Boolean isActive;
}
