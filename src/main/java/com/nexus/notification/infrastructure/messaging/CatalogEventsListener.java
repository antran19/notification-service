package com.nexus.notification.infrastructure.messaging;

import com.nexus.common.events.DomainEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CatalogEventsListener {

    // None of the 5 mapped event types come from catalog-service -- every row recorded
    // from this topic is audit-only (null recipient/message) per the spec's scope.
    private static final Map<String, Class<? extends DomainEvent>> MAPPED_TYPES = Map.of();

    private final EventRecordingService eventRecordingService;

    public CatalogEventsListener(EventRecordingService eventRecordingService) {
        this.eventRecordingService = eventRecordingService;
    }

    @KafkaListener(topics = "catalog-events")
    public void onMessage(String rawPayload) {
        eventRecordingService.process(rawPayload, MAPPED_TYPES);
    }
}
