package com.example.tracking_order.modules.order.event;

import java.math.BigDecimal;

public record OrderCreatedApplicationEvent(
        Long orderId,
        String orderCode,
        Long userId,
        BigDecimal grandTotal) {
}
