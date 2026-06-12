package com.example.tracking_order.modules.notification.consumer;

import com.example.tracking_order.modules.notification.enums.NotificationType;
import com.example.tracking_order.modules.notification.service.ResilientNotificationService;
import com.example.tracking_order.modules.order.event.OrderCreatedEvent;
import com.example.tracking_order.modules.user.entity.User;
import com.example.tracking_order.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderCreatedNotificationConsumer {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final ResilientNotificationService notificationService;

    @Value("${app.kafka.consumer-idempotency-ttl-hours:168}")
    private long idempotencyTtlHours;

    @KafkaListener(
            topics = "${app.kafka.topics.order-created:order.created}",
            groupId = "${spring.kafka.consumer.group-id:tracking-order-notification}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        String idempotencyKey = "kafka:processed:" + event.eventId();
        Boolean firstSeen = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "1", Duration.ofHours(idempotencyTtlHours));

        if (Boolean.FALSE.equals(firstSeen)) {
            log.info("Skipping duplicate order.created event. eventId={}", event.eventId());
            return;
        }

        User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new IllegalStateException("User not found for order.created event: " + event.userId()));

        notificationService.sendNotification(
                user,
                "Order created",
                "Order " + event.orderCode() + " was created successfully.",
                NotificationType.ORDER_STATUS,
                "ORDER",
                event.orderId());
    }
}
