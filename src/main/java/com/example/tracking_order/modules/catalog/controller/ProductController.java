package com.example.tracking_order.modules.catalog.controller;

import com.example.tracking_order.common.response.ApiResponse;
import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.catalog.dto.*;
import com.example.tracking_order.modules.catalog.enums.ProductStatus;
import com.example.tracking_order.modules.catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageData<ProductListDto>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Long category_id,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) BigDecimal min_price,
            @RequestParam(required = false) BigDecimal max_price,
            @RequestParam(required = false) Long seller_id,
            @RequestParam(required = false) Boolean featured, // TODO: logic for featured
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(productService.getProducts(search, sku, category_id, status, min_price, max_price, seller_id, sort, page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ApiResponse<ProductDetailDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success(productService.createProduct(request));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailDto> getProductDetail(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProductDetail(productId));
    }

    @PatchMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ApiResponse<ProductDetailDto> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.success(productService.updateProduct(productId, request));
    }

    @GetMapping("/{productId}/inventory")
    public ApiResponse<InventoryDto> getInventory(@PathVariable Long productId) {
        return ApiResponse.success(productService.getInventory(productId));
    }

    @PatchMapping("/{productId}/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InventoryDto> updateInventory(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        return ApiResponse.success(productService.updateInventory(productId, request));
    }
}
