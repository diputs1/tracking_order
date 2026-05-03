package com.example.tracking_order.modules.cart.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartDto {
    private Long cartId;
    private List<CartItemDto> items;
    private Summary summary;

    @Data
    @Builder
    public static class CartItemDto {
        private Long item_id;
        private Long product_id;
        private String product_name;
        private String product_image;
        private String product_sku;
        private BigDecimal price_snapshot;
        private BigDecimal current_price;
        private Integer quantity;
        private Integer quantity_in_stock;
        private Boolean is_available;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    public static class Summary {
        private Integer item_count;
        private Integer total_qty;
        private BigDecimal subtotal;
        private Boolean has_out_of_stock;
    }
}
