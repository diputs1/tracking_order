package com.example.tracking_order.modules.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("related_entity_type")
    private String relatedEntityType;
    @JsonProperty("related_entity_id")
    private Long relatedEntityId;
    @JsonProperty("is_read")
    private Boolean isRead;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
