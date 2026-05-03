package com.example.tracking_order.modules.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ReturnRequestDto {
    @NotBlank(message = "Lý do trả hàng không được để trống")
    private String reason;
    
    @JsonProperty("proof_images")
    private List<String> proofImages;
}
