package com.nexus.notification.api.mapper;

import com.nexus.notification.api.dto.NotificationResponse;
import com.nexus.notification.domain.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationApiMapper {
    NotificationResponse toResponse(Notification notification);
}
