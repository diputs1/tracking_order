package com.example.tracking_order.modules.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCartItemRequest {
    @NotNull(message = "Quantity không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer quantity;
}
