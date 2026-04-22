package com.example.tracking_order.modules.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductListDto {
    private Long id;
    private String name;
    private String sku;
    private String slug;
    private BigDecimal base_price;
    private BigDecimal sale_price;
    private String status;
    private CategoryRef category;
    private SellerRef seller;
    private InventoryRef inventory;
    private String thumbnail_url;
    private BigDecimal rating_avg;

    @Data
    @Builder
    public static class CategoryRef {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class SellerRef {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    public static class InventoryRef {
        private Integer quantity_in_stock;
        private Integer quantity_available;
    }
}
