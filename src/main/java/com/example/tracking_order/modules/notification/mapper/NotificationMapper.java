package com.example.tracking_order.modules.notification.mapper;

import com.example.tracking_order.modules.notification.dto.NotificationDto;
import com.example.tracking_order.modules.notification.entity.NotificationLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(target = "type", expression = "java(notificationLog.getType().name())")
    NotificationDto toNotificationDto(NotificationLog notificationLog);

    List<NotificationDto> toNotificationDtoList(List<NotificationLog> notificationLogs);
}
