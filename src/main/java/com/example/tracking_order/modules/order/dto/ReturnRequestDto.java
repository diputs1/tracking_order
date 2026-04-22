package com.example.tracking_order.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ReturnRequestDto {
    @NotBlank(message = "Lý do trả hàng không được để trống")
    private String reason;
    
    private List<String> proof_images;
}
