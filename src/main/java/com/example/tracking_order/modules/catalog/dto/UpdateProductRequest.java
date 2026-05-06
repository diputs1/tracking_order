package com.example.tracking_order.modules.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.tracking_order.modules.catalog.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateProductRequest {
    private String name;
    @JsonProperty("base_price")
    private BigDecimal basePrice;
    @JsonProperty("sale_price")
    private BigDecimal salePrice;
    private String description;
    private ProductStatus status;
    @JsonProperty("image_url")
    private String imageUrl;
    private List<String> images;
    private BigDecimal weight;
}
