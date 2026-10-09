package com.nexus.notification.infrastructure.messaging;

import com.nexus.common.events.DomainEvent;
import com.nexus.common.events.PasswordResetRequestedEvent;
import com.nexus.common.events.UserRegisteredEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserEventsListener {

    private static final Map<String, Class<? extends DomainEvent>> MAPPED_TYPES = Map.of(
            "UserRegistered", UserRegisteredEvent.class,
            "PasswordResetRequested", PasswordResetRequestedEvent.class);

    private final EventRecordingService eventRecordingService;

    public UserEventsListener(EventRecordingService eventRecordingService) {
        this.eventRecordingService = eventRecordingService;
    }

    @KafkaListener(topics = "user-events")
    public void onMessage(String rawPayload) {
        eventRecordingService.process(rawPayload, MAPPED_TYPES);
    }
}
