package com.example.tracking_order.modules.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShippingCarrierRequest {
    @NotBlank(message = "Tên đơn vị vận chuyển không được để trống")
    private String name;
    
    @NotBlank(message = "Mã đơn vị vận chuyển không được để trống")
    private String code;
    
    private String trackingUrlTemplate;
    
    private Boolean isActive = true;
}
