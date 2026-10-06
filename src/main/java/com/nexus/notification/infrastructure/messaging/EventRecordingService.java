package com.nexus.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.events.DomainEvent;
import com.nexus.notification.application.usecase.RecordNotificationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class EventRecordingService {

    private static final Logger log = LoggerFactory.getLogger(EventRecordingService.class);

    private final ObjectMapper objectMapper;
    private final RecordNotificationUseCase recordNotificationUseCase;

    public EventRecordingService(ObjectMapper objectMapper, RecordNotificationUseCase recordNotificationUseCase) {
        this.objectMapper = objectMapper;
        this.recordNotificationUseCase = recordNotificationUseCase;
    }

    public void process(String rawPayload, Map<String, Class<? extends DomainEvent>> mappedTypes) {
        try {
            JsonNode node = objectMapper.readTree(rawPayload);
            String eventId = node.get("eventId").asText();
            String eventType = node.get("eventType").asText();
            String aggregateId = node.hasNonNull("aggregateId") ? node.get("aggregateId").asText() : null;
            Instant occurredAt = Instant.parse(node.get("occurredAt").asText());

            Class<? extends DomainEvent> mappedClass = mappedTypes.get(eventType);
            // readValue directly from the raw JSON string, not treeToValue(node, ...): the
            // JsonNode tree above represents a decimal number as a DoubleNode by default,
            // and converting that back to BigDecimal loses the original scale (500.00
            // becomes 500.0). Parsing straight from the string avoids that lossy detour.
            DomainEvent typedEvent = mappedClass == null ? null : objectMapper.readValue(rawPayload, mappedClass);

            recordNotificationUseCase.record(eventId, eventType, aggregateId, occurredAt, rawPayload, typedEvent);
        } catch (Exception e) {
            // Deliberately broad: a malformed message on the topic must not stop this
            // listener's partition from consuming later messages.
            log.error("Failed to process event payload, skipping: {}", rawPayload, e);
        }
    }
}
