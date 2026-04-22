package com.example.tracking_order.modules.notification.service;

import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.notification.dto.NotificationDto;
import com.example.tracking_order.modules.notification.enums.NotificationType;
import com.example.tracking_order.modules.user.entity.User;

public interface NotificationService {
    void sendNotification(User user, String title, String message, NotificationType type, String relatedEntityType, Long relatedEntityId);
    PageData<NotificationDto> getMyNotifications(int page, int size);
    void markAsRead(Long id);
}
