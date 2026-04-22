package com.example.tracking_order.modules.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryDto {
    private Long product_id;
    private Integer quantity_in_stock;
    private Integer quantity_reserved;
    private Integer quantity_available;
    private Boolean is_available;
    private LocalDateTime updated_at;
}
