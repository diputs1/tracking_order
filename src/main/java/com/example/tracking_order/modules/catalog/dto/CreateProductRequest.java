package com.example.tracking_order.modules.catalog.dto;

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
    private Long category_id;

    @NotNull(message = "Giá gốc không được để trống")
    @Min(value = 0, message = "Giá gốc không hợp lệ")
    private BigDecimal base_price;

    private BigDecimal sale_price;
    private String description;
    private BigDecimal weight;

    @NotNull(message = "Tồn kho ban đầu không được để trống")
    @Min(value = 0, message = "Tồn kho không hợp lệ")
    private Integer initial_stock;

    private List<String> images;
    private ProductStatus status;
}
