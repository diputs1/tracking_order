package com.example.tracking_order.modules.notification.mapper;

import com.example.tracking_order.modules.notification.dto.NotificationDto;
import com.example.tracking_order.modules.notification.entity.NotificationLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(target = "related_entity_type", source = "relatedEntityType")
    @Mapping(target = "related_entity_id", source = "relatedEntityId")
    @Mapping(target = "is_read", source = "isRead")
    @Mapping(target = "created_at", source = "createdAt")
    NotificationDto toNotificationDto(NotificationLog notificationLog);
}
