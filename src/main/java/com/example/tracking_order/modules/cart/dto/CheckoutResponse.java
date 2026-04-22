package com.example.tracking_order.modules.cart.dto;

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
    private String payment_url;

    @Data
    @Builder
    public static class OrderSummary {
        private Long id;
        private String order_code;
        private String status;
        private String payment_status;
        private BigDecimal subtotal;
        private BigDecimal discount_amount;
        private BigDecimal shipping_fee;
        private BigDecimal grand_total;
        private LocalDateTime estimated_delivery_at;
    }
}
