package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.NotificationRepositoryPort;

public class CountMyUnreadNotificationsUseCase {
    private final NotificationRepositoryPort repository;

    public CountMyUnreadNotificationsUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    public long count(String recipientUserId) {
        return repository.countUnreadByRecipient(recipientUserId);
    }
}
