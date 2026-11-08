package com.nexus.notification.application.usecase;

import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ValidationException;
import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.domain.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class ListMyNotificationsUseCase {
    public static final int MAX_SIZE = 100;

    private final NotificationRepositoryPort repository;

    public ListMyNotificationsUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    public List<Notification> list(String recipientUserId, boolean unreadOnly, int page, int size) {
        List<FieldError> errors = new ArrayList<>();
        if (page < 0) {
            errors.add(new FieldError("page", "Page must be greater than or equal to 0"));
        }
        if (size < 1 || size > MAX_SIZE) {
            errors.add(new FieldError("size", "Size must be between 1 and " + MAX_SIZE));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return repository.findPageByRecipient(recipientUserId, unreadOnly, page, size);
    }
}
