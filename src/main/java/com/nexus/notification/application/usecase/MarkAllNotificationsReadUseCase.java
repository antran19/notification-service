package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.NotificationRepositoryPort;

public class MarkAllNotificationsReadUseCase {
    private final NotificationRepositoryPort repository;

    public MarkAllNotificationsReadUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    // Returns how many were unread before. Calling it again is harmless and returns 0.
    public int markAllRead(String recipientUserId) {
        return repository.markAllReadByRecipient(recipientUserId);
    }
}
