package com.nexus.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Notification {

    private final String id;
    private final String eventId;
    private final String eventType;
    private final String aggregateId;
    private final String recipientUserId;
    private final String message;
    private final String payload;
    private boolean read;
    private final Instant occurredAt;
    private final Instant createdAt;

    public Notification(String id, String eventId, String eventType, String aggregateId,
                         String recipientUserId, String message, String payload,
                         boolean read, Instant occurredAt, Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.recipientUserId = recipientUserId;
        this.message = message;
        this.payload = payload;
        this.read = read;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public static Notification record(String eventId, String eventType, String aggregateId,
                                       String recipientUserId, String message, String payload,
                                       Instant occurredAt) {
        return new Notification(UUID.randomUUID().toString(), eventId, eventType, aggregateId,
                recipientUserId, message, payload, false, occurredAt, Instant.now());
    }

    public void markRead() {
        this.read = true;
    }

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getRecipientUserId() { return recipientUserId; }
    public String getMessage() { return message; }
    public String getPayload() { return payload; }
    public boolean isRead() { return read; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
}
