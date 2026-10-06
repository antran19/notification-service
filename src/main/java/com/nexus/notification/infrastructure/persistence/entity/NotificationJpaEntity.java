package com.nexus.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(name = "recipient_user_id")
    private String recipientUserId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationJpaEntity() {
    }

    public NotificationJpaEntity(UUID id, String eventId, String eventType, String aggregateId,
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

    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getRecipientUserId() { return recipientUserId; }
    public String getMessage() { return message; }
    public String getPayload() { return payload; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
}
