package com.example.tracking_order.modules.cart.dto;

import com.example.tracking_order.modules.payment.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuickCheckoutRequest {
    @NotNull(message = "Product ID không được để trống")
    private Long product_id;

    @NotNull(message = "Quantity không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @NotNull(message = "Address ID không được để trống")
    private Long address_id;

    @NotNull(message = "Carrier ID không được để trống")
    private Long carrier_id;

    @NotNull(message = "Payment Method không được để trống")
    private PaymentMethod payment_method;

    private String discount_code;
}
