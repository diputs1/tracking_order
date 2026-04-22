package com.example.tracking_order.modules.order.dto;

import com.example.tracking_order.modules.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotNull(message = "Trạng thái không được để trống")
    private OrderStatus status;

    private String note;
}
