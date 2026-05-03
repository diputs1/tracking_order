package com.example.tracking_order.modules.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutResponse {
    private OrderSummary order;
    @JsonProperty("payment_url")
    private String paymentUrl;

    @Data
    @Builder
    public static class OrderSummary {
        private Long id;
        @JsonProperty("order_code")
        private String orderCode;
        private String status;
        @JsonProperty("payment_status")
        private String paymentStatus;
        private BigDecimal subtotal;
        @JsonProperty("discount_amount")
        private BigDecimal discountAmount;
        @JsonProperty("shipping_fee")
        private BigDecimal shippingFee;
        @JsonProperty("grand_total")
        private BigDecimal grandTotal;
        @JsonProperty("estimated_delivery_at")
        private LocalDateTime estimatedDeliveryAt;
    }
}
