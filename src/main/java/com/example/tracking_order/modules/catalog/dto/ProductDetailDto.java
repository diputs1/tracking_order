package com.example.tracking_order.modules.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("base_price")
    private BigDecimal basePrice;
    @JsonProperty("sale_price")
    private BigDecimal salePrice;
    private BigDecimal weight;
    private String status;
    private CategoryRef category;
    private SellerRef seller;
    private List<String> images;
    private InventoryRef inventory;
    @JsonProperty("rating_avg")
    private BigDecimal ratingAvg;
    @JsonProperty("review_count")
    private Integer reviewCount;

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
        @JsonProperty("quantity_in_stock")
        private Integer quantityInStock;
        @JsonProperty("quantity_available")
        private Integer quantityAvailable;
    }
}
