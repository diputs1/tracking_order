package com.example.tracking_order.modules.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductDetailDto {
    private Long id;
    private String name;
    private String sku;
    private String slug;
    private String description;
    private BigDecimal base_price;
    private BigDecimal sale_price;
    private BigDecimal weight;
    private String status;
    private CategoryRef category;
    private SellerRef seller;
    private List<String> images;
    private InventoryRef inventory;
    private BigDecimal rating_avg;
    private Integer review_count;

    @Data
    @Builder
    public static class CategoryRef {
        private Long id;
        private String name;
        private String slug;
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
