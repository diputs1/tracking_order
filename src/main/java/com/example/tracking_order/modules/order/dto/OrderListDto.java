package com.example.tracking_order.modules.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderListDto {
    private Long id;
    private String order_code;
    private BigDecimal grand_total;
    private String status;
    private String payment_status;
    private String payment_method;
    private Integer item_count;
    private LocalDateTime created_at;
}
