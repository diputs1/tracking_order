package com.example.tracking_order.modules.notification.service.impl;

import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.common.response.PageData;
import com.example.tracking_order.modules.notification.dto.NotificationDto;
import com.example.tracking_order.modules.notification.entity.NotificationLog;
import com.example.tracking_order.modules.notification.enums.NotificationType;
import com.example.tracking_order.modules.notification.repository.NotificationLogRepository;
import com.example.tracking_order.modules.notification.service.NotificationService;
import com.example.tracking_order.modules.user.entity.User;
import com.example.tracking_order.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    @Override
    @Async
    public void sendNotification(User user, String title, String message, NotificationType type, String relatedEntityType, Long relatedEntityId) {
        log.info("Sending async notification to user {}: {}", user.getEmail(), title);
        
        NotificationLog notification = NotificationLog.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .build();
                
        notificationLogRepository.save(notification);
    }

    @Override
    public PageData<NotificationDto> getMyNotifications(int page, int size) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<NotificationLog> notifPage = notificationLogRepository.findByUserIdOrderByCreatedAtDesc(
                userDetails.getId(), PageRequest.of(page - 1, size));

        List<NotificationDto> items = notifPage.getContent().stream().map(n -> NotificationDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType().name())
                .relatedEntityType(n.getRelatedEntityType())
                .relatedEntityId(n.getRelatedEntityId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build()).collect(Collectors.toList());

        PageData.Pagination pagination = PageData.Pagination.builder()
                .page(page)
                .totalPages(notifPage.getTotalPages())
                .totalItems(notifPage.getTotalElements())
                .build();

        return PageData.<NotificationDto>builder().items(items).pagination(pagination).build();
    }

    @Override
    public void markAsRead(Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        NotificationLog notif = notificationLogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Thông báo không tồn tại"));

        if (!notif.getUser().getId().equals(userDetails.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Không có quyền truy cập thông báo này");
        }

        notif.setIsRead(true);
        notificationLogRepository.save(notif);
    }
}
