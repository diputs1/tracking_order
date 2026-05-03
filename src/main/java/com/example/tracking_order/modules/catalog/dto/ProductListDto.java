package com.example.tracking_order.modules.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("base_price")
    private BigDecimal basePrice;
    @JsonProperty("sale_price")
    private BigDecimal salePrice;
    private String status;
    private CategoryRef category;
    private SellerRef seller;
    private InventoryRef inventory;
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
    @JsonProperty("rating_avg")
    private BigDecimal ratingAvg;

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
        @JsonProperty("quantity_in_stock")
        private Integer quantityInStock;
        @JsonProperty("quantity_available")
        private Integer quantityAvailable;
    }
}
