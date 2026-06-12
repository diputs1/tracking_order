package com.example.tracking_order.modules.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.order-created:order.created}")
    private String orderCreatedTopic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOrderCreated(OrderCreatedApplicationEvent applicationEvent) {
        OrderCreatedEvent event = OrderCreatedEvent.from(applicationEvent);
        kafkaTemplate.send(orderCreatedTopic, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish order.created event. orderId={}, eventId={}",
                                event.orderId(), event.eventId(), ex);
                        return;
                    }
                    log.info("Published order.created event. orderId={}, eventId={}",
                            event.orderId(), event.eventId());
                });
    }
}
