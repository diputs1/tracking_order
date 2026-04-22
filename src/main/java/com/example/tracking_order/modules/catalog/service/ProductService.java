package com.example.tracking_order.modules.catalog.service;

import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.catalog.dto.*;
import com.example.tracking_order.modules.catalog.enums.ProductStatus;

import java.math.BigDecimal;

public interface ProductService {
    PageData<ProductListDto> getProducts(String search, String sku, Long categoryId, ProductStatus status,
                                         BigDecimal minPrice, BigDecimal maxPrice, Long sellerId,
                                         String sort, int page, int size);

    ProductDetailDto createProduct(CreateProductRequest request);

    ProductDetailDto getProductDetail(Long productId);

    ProductDetailDto updateProduct(Long productId, UpdateProductRequest request);

    InventoryDto getInventory(Long productId);

    InventoryDto updateInventory(Long productId, UpdateInventoryRequest request);
}
