package com.nexus.notification.application.usecase;

import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.common.core.exception.NotFoundException;
import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.domain.model.Notification;

import java.util.Objects;

public class MarkNotificationReadUseCase {
    private final NotificationRepositoryPort repository;

    public MarkNotificationReadUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    public void markRead(String notificationId, String callerUserId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND",
                        "Notification not found: " + notificationId));

        if (!Objects.equals(notification.getRecipientUserId(), callerUserId)) {
            throw new ForbiddenException("NOT_YOUR_NOTIFICATION", "You do not own this notification");
        }

        notification.markRead();
        repository.update(notification);
    }
}
