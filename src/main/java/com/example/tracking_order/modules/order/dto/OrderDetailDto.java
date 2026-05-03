package com.example.tracking_order.modules.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailDto {
    private Long id;
    @JsonProperty("order_code")
    private String orderCode;
    private String status;
    @JsonProperty("payment_status")
    private String paymentStatus;
    @JsonProperty("payment_method")
    private String paymentMethod;
    private BigDecimal subtotal;
    @JsonProperty("discount_amount")
    private BigDecimal discountAmount;
    @JsonProperty("shipping_fee")
    private BigDecimal shippingFee;
    @JsonProperty("grand_total")
    private BigDecimal grandTotal;
    private ShippingInfo shipping;
    private List<OrderItemDto> items;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class ShippingInfo {
        @JsonProperty("recipient_name")
        private String recipientName;
        @JsonProperty("recipient_phone")
        private String recipientPhone;
        private String address;
        @JsonProperty("carrier_name")
        private String carrierName;
        @JsonProperty("tracking_number")
        private String trackingNumber;
        @JsonProperty("tracking_url")
        private String trackingUrl;
    }

    @Data
    @Builder
    public static class OrderItemDto {
        @JsonProperty("product_id")
        private Long productId;
        @JsonProperty("product_name")
        private String productName;
        @JsonProperty("product_sku")
        private String productSku;
        @JsonProperty("unit_price")
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}
