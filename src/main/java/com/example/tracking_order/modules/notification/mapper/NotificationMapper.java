package com.example.tracking_order.modules.notification.mapper;

import com.example.tracking_order.modules.notification.dto.NotificationDto;
import com.example.tracking_order.modules.notification.entity.NotificationLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(target = "relatedEntityType", source = "relatedEntityType")
    @Mapping(target = "relatedEntityId", source = "relatedEntityId")
    @Mapping(target = "isRead", source = "isRead")
    @Mapping(target = "createdAt", source = "createdAt")
    NotificationDto toNotificationDto(NotificationLog notificationLog);
}
