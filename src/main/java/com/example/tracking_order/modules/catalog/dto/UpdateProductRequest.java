package com.example.tracking_order.modules.catalog.dto;

import com.example.tracking_order.modules.catalog.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateProductRequest {
    private String name;
    private BigDecimal base_price;
    private BigDecimal sale_price;
    private String description;
    private ProductStatus status;
    private List<String> images;
    private BigDecimal weight;
}
