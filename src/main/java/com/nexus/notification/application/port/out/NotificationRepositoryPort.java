package com.nexus.notification.application.port.out;

import com.nexus.notification.domain.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    // true when the notification was stored, false when one for the same event id already existed.
    boolean record(Notification notification);
    void update(Notification notification);
    List<Notification> findByRecipientUserId(String recipientUserId);
    List<Notification> findAllFiltered(String eventType, String aggregateId);
    Optional<Notification> findById(String id);
}
