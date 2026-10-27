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

    public NotificationMessageResolver.Resolution record(String eventId, String eventType, String aggregateId,
                                                           Instant occurredAt, String rawPayload,
                                                           DomainEvent typedEventOrNull) {
        NotificationMessageResolver.Resolution resolution = typedEventOrNull == null
                ? NotificationMessageResolver.Resolution.UNMAPPED
                : resolver.resolve(eventType, typedEventOrNull);

        Notification notification = Notification.record(eventId, eventType, aggregateId,
                resolution.recipientUserId(), resolution.message(), rawPayload, occurredAt);
        // Kafka delivers at least once. A redelivered event leaves the stored notification
        // untouched (event_id is unique), and must not trigger the email a second time either.
        boolean stored = repository.record(notification);
        return stored ? resolution : NotificationMessageResolver.Resolution.ALREADY_RECORDED;
    }
}
