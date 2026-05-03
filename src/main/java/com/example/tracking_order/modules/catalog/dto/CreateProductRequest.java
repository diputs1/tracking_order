package com.example.tracking_order.modules.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.tracking_order.modules.catalog.enums.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "SKU không được để trống")
    private String sku;

    @NotNull(message = "Danh mục không được để trống")
    @JsonProperty("category_id")
    private Long categoryId;

    @NotNull(message = "Giá gốc không được để trống")
    @Min(value = 0, message = "Giá gốc không hợp lệ")
    @JsonProperty("base_price")
    private BigDecimal basePrice;

    @JsonProperty("sale_price")
    private BigDecimal salePrice;
    private String description;
    private BigDecimal weight;

    @NotNull(message = "Tồn kho ban đầu không được để trống")
    @Min(value = 0, message = "Tồn kho không hợp lệ")
    @JsonProperty("initial_stock")
    private Integer initialStock;

    private List<String> images;
    private ProductStatus status;
}
