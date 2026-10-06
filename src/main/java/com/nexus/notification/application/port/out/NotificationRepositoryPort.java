package com.nexus.notification.application.port.out;

import com.nexus.notification.domain.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    void record(Notification notification);
    void update(Notification notification);
    List<Notification> findByRecipientUserId(String recipientUserId);
    List<Notification> findAllFiltered(String eventType, String aggregateId);
    Optional<Notification> findById(String id);
}
