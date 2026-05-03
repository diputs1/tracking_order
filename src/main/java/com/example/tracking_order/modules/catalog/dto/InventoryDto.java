package com.example.tracking_order.modules.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryDto {
    @JsonProperty("product_id")
    private Long productId;
    @JsonProperty("quantity_in_stock")
    private Integer quantityInStock;
    @JsonProperty("quantity_reserved")
    private Integer quantityReserved;
    @JsonProperty("quantity_available")
    private Integer quantityAvailable;
    @JsonProperty("is_available")
    private Boolean isAvailable;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
