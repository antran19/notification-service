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
        EventEnvelope envelope;
        try {
            envelope = parseEnvelope(rawPayload, mappedTypes);
        } catch (Exception e) {
            // Deliberately broad, but ONLY around parsing: a malformed message on the
            // topic must not stop this listener's partition from consuming later
            // messages. A failure recording the (successfully parsed) event -- e.g. the
            // database being unreachable -- is NOT caught here; it propagates out of
            // process() so Spring Kafka's consumer error handler retries/backs off,
            // instead of the event being silently and permanently lost.
            log.error("Failed to parse event payload, skipping: {}", rawPayload, e);
            return;
        }
        recordNotificationUseCase.record(envelope.eventId(), envelope.eventType(), envelope.aggregateId(),
                envelope.occurredAt(), rawPayload, envelope.typedEvent());
    }

    private EventEnvelope parseEnvelope(String rawPayload, Map<String, Class<? extends DomainEvent>> mappedTypes)
            throws com.fasterxml.jackson.core.JsonProcessingException {
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

        return new EventEnvelope(eventId, eventType, aggregateId, occurredAt, typedEvent);
    }

    private record EventEnvelope(String eventId, String eventType, String aggregateId, Instant occurredAt,
                                  DomainEvent typedEvent) {
    }
}
