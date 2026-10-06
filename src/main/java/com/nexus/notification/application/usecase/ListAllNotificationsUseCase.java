package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.domain.model.Notification;

import java.util.List;

public class ListAllNotificationsUseCase {
    private final NotificationRepositoryPort repository;

    public ListAllNotificationsUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    public List<Notification> list(String eventType, String aggregateId) {
        return repository.findAllFiltered(eventType, aggregateId);
    }
}
