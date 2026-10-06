package com.nexus.notification.application.usecase;

import com.nexus.common.events.DomainEvent;
import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.application.service.NotificationMessageResolver;
import com.nexus.notification.domain.model.Notification;

import java.time.Instant;

public class RecordNotificationUseCase {

    private final NotificationRepositoryPort repository;
    private final NotificationMessageResolver resolver;

    public RecordNotificationUseCase(NotificationRepositoryPort repository, NotificationMessageResolver resolver) {
        this.repository = repository;
        this.resolver = resolver;
    }

    public void record(String eventId, String eventType, String aggregateId, Instant occurredAt,
                        String rawPayload, DomainEvent typedEventOrNull) {
        NotificationMessageResolver.Resolution resolution = typedEventOrNull == null
                ? NotificationMessageResolver.Resolution.UNMAPPED
                : resolver.resolve(eventType, typedEventOrNull);

        Notification notification = Notification.record(eventId, eventType, aggregateId,
                resolution.recipientUserId(), resolution.message(), rawPayload, occurredAt);
        repository.record(notification);
    }
}
