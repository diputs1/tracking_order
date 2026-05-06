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
import com.example.tracking_order.modules.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Async
    @Transactional
    public void sendNotification(User user, String title, String message, NotificationType type, String relatedEntityType, Long relatedEntityId) {
        log.info("Sending async notification to userId {}: {}", user.getId(), title);
        
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
    @Transactional(readOnly = true)
    public PageData<NotificationDto> getMyNotifications(int page, int size) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<NotificationLog> notifPage = notificationLogRepository.findByUserIdOrderByCreatedAtDesc(
                userDetails.getId(), PageRequest.of(page - 1, size));

        List<NotificationDto> items = notificationMapper.toNotificationDtoList(notifPage.getContent());

        PageData.Pagination pagination = PageData.Pagination.builder()
                .page(page)
                .totalPages(notifPage.getTotalPages())
                .totalItems(notifPage.getTotalElements())
                .build();

        return PageData.<NotificationDto>builder().items(items).pagination(pagination).build();
    }

    @Override
    @Transactional
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
