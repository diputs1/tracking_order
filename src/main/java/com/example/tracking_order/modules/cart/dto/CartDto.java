package com.example.tracking_order.modules.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartDto {
    private Long cartId;
    @JsonProperty("user_id")
    private Long userId;
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;
    private List<CartItemDto> items;
    private Summary summary;

    @Data
    @Builder
    public static class CartItemDto {
        @JsonProperty("item_id")
        private Long itemId;
        @JsonProperty("product_id")
        private Long productId;
        @JsonProperty("product_name")
        private String productName;
        @JsonProperty("product_image")
        private String productImage;
        @JsonProperty("product_sku")
        private String productSku;
        @JsonProperty("price_snapshot")
        private BigDecimal priceSnapshot;
        @JsonProperty("current_price")
        private BigDecimal currentPrice;
        private Integer quantity;
        @JsonProperty("quantity_in_stock")
        private Integer quantityInStock;
        @JsonProperty("is_available")
        private Boolean isAvailable;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    public static class Summary {
        @JsonProperty("item_count")
        private Integer itemCount;
        @JsonProperty("total_qty")
        private Integer totalQty;
        private BigDecimal subtotal;
        @JsonProperty("has_out_of_stock")
        private Boolean hasOutOfStock;
    }
}
