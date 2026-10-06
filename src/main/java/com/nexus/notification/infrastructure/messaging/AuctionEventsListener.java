package com.nexus.notification.infrastructure.messaging;

import com.nexus.common.events.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuctionEventsListener {

    private static final Map<String, Class<? extends DomainEvent>> MAPPED_TYPES = Map.of(
            "BidPlaced", BidPlacedEvent.class,
            "Outbid", OutbidEvent.class,
            "AuctionWon", AuctionWonEvent.class,
            "AuctionSettled", AuctionSettledEvent.class);

    private final EventRecordingService eventRecordingService;

    public AuctionEventsListener(EventRecordingService eventRecordingService) {
        this.eventRecordingService = eventRecordingService;
    }

    @KafkaListener(topics = "auction-events")
    public void onMessage(String rawPayload) {
        eventRecordingService.process(rawPayload, MAPPED_TYPES);
    }
}
