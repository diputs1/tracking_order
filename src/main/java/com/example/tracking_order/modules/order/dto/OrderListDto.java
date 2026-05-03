package com.example.tracking_order.modules.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderListDto {
    private Long id;
    @JsonProperty("order_code")
    private String orderCode;
    @JsonProperty("grand_total")
    private BigDecimal grandTotal;
    private String status;
    @JsonProperty("payment_status")
    private String paymentStatus;
    @JsonProperty("payment_method")
    private String paymentMethod;
    @JsonProperty("item_count")
    private Integer itemCount;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
