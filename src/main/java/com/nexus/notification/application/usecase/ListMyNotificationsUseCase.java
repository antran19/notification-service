package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.domain.model.Notification;

import java.util.List;

public class ListMyNotificationsUseCase {
    private final NotificationRepositoryPort repository;

    public ListMyNotificationsUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    public List<Notification> list(String recipientUserId) {
        return repository.findByRecipientUserId(recipientUserId);
    }
}
