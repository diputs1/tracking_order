package com.example.tracking_order.modules.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailDto {
    private Long id;
    private String order_code;
    private String status;
    private String payment_status;
    private String payment_method;
    private BigDecimal subtotal;
    private BigDecimal discount_amount;
    private BigDecimal shipping_fee;
    private BigDecimal grand_total;
    private ShippingInfo shipping;
    private List<OrderItemDto> items;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    @Data
    @Builder
    public static class ShippingInfo {
        private String recipient_name;
        private String recipient_phone;
        private String address;
        private String carrier_name;
        private String tracking_number;
        private String tracking_url;
    }

    @Data
    @Builder
    public static class OrderItemDto {
        private Long product_id;
        private String product_name;
        private String product_sku;
        private BigDecimal unit_price;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}
