package com.example.tracking_order.modules.notification.service;

import com.example.tracking_order.modules.notification.enums.NotificationType;
import com.example.tracking_order.modules.user.entity.User;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientNotificationService {

    private final NotificationService notificationService;

    @Retry(name = "notification")
    @CircuitBreaker(name = "notification", fallbackMethod = "notificationFallback")
    public void sendNotification(User user, String title, String message, NotificationType type,
            String relatedEntityType, Long relatedEntityId) {
        notificationService.sendNotification(user, title, message, type, relatedEntityType, relatedEntityId);
    }

    void notificationFallback(User user, String title, String message, NotificationType type,
            String relatedEntityType, Long relatedEntityId, Throwable throwable) {
        log.warn("Notification delivery skipped after resilience policy. userId={}, type={}, entityType={}, entityId={}",
                user != null ? user.getId() : null, type, relatedEntityType, relatedEntityId, throwable);
    }
}
