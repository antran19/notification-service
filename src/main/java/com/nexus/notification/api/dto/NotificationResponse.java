package com.nexus.notification.api.dto;

import java.time.Instant;

public record NotificationResponse(
        String id, String eventType, String aggregateId, String recipientUserId,
        String message, boolean read, String payload, Instant occurredAt) {
}
