package com.example.tracking_order.modules.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        String orderCode,
        Long userId,
        BigDecimal grandTotal,
        Instant occurredAt) {

    public static OrderCreatedEvent from(OrderCreatedApplicationEvent event) {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                event.orderId(),
                event.orderCode(),
                event.userId(),
                event.grandTotal(),
                Instant.now());
    }
}
