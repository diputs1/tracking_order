package com.example.tracking_order.modules.catalog.dto;

import java.math.BigDecimal;

public interface ProductProjection {
    Long getId();
    String getName();
    String getSku();
    String getSlug();
    BigDecimal getBasePrice();
    BigDecimal getSalePrice();
    String getStatus();
    
    // Lấy thông tin Category (chỉ name và id)
    Long getCategoryId();
    String getCategoryName();
    
    // Lấy thông tin Seller
    Long getSellerId();
    String getSellerFullName();
    
    // Lấy thông tin Kho
    Integer getQuantityInStock();
    Integer getQuantityReserved();
}
