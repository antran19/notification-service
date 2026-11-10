package com.nexus.notification.infrastructure.messaging;

import com.nexus.common.events.DomainEvent;
import com.nexus.common.events.OrderPaidEvent;
import com.nexus.common.events.OrderShippedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

// Order events from commerce-service. Only the ones with a clear recipient become notifications;
// the rest of the topic is recorded as unmapped and ignored.
@Component
public class CommerceEventsListener {

    private static final Map<String, Class<? extends DomainEvent>> MAPPED_TYPES = Map.of(
            "OrderPaid", OrderPaidEvent.class,
            "OrderShipped", OrderShippedEvent.class);

    private final EventRecordingService eventRecordingService;

    public CommerceEventsListener(EventRecordingService eventRecordingService) {
        this.eventRecordingService = eventRecordingService;
    }

    @KafkaListener(topics = "commerce-events")
    public void onMessage(String rawPayload) {
        eventRecordingService.process(rawPayload, MAPPED_TYPES);
    }
}
