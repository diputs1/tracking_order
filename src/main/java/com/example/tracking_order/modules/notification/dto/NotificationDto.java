package com.example.tracking_order.modules.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDto {
    private Long id;
    private String title;
    private String message;
    private String type;
    private String related_entity_type;
    private Long related_entity_id;
    private Boolean is_read;
    private LocalDateTime created_at;
}
