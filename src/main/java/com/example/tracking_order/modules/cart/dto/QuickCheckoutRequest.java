package com.example.tracking_order.modules.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.tracking_order.modules.payment.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuickCheckoutRequest {
    @NotNull(message = "Product ID không được để trống")
    @JsonProperty("product_id")
    private Long productId;

    @NotNull(message = "Quantity không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @NotNull(message = "Address ID không được để trống")
    @JsonProperty("address_id")
    private Long addressId;

    @NotNull(message = "Carrier ID không được để trống")
    @JsonProperty("carrier_id")
    private Long carrierId;

    @NotNull(message = "Payment Method không được để trống")
    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;

    @JsonProperty("discount_code")
    private String discountCode;
}
