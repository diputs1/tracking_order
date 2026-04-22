package com.example.tracking_order.modules.cart.dto;

import com.example.tracking_order.modules.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {
    @NotNull(message = "Address ID không được để trống")
    private Long address_id;

    @NotNull(message = "Carrier ID không được để trống")
    private Long carrier_id;

    @NotNull(message = "Payment Method không được để trống")
    private PaymentMethod payment_method;

    private String discount_code;
    
    private String note;
}
