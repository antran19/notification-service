package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.BrokerHealthProbePort;
import com.nexus.notification.application.port.out.MessageConsumptionControlPort;
import com.nexus.notification.application.service.BrokerHealthTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

// SRS 3.2.2 Message Broker Health Check. The outage event is written straight to the event
// log (notifications table) rather than published: the broker being unreachable is exactly
// when publishing would fail.
public class CheckBrokerHealthUseCase {

    private static final Logger log = LoggerFactory.getLogger(CheckBrokerHealthUseCase.class);
    private static final String AGGREGATE = "kafka";

    private final BrokerHealthProbePort probe;
    private final BrokerHealthTracker tracker;
    private final RecordNotificationUseCase recordNotificationUseCase;
    private final MessageConsumptionControlPort consumption;

    public CheckBrokerHealthUseCase(BrokerHealthProbePort probe, BrokerHealthTracker tracker,
                                     RecordNotificationUseCase recordNotificationUseCase,
                                     MessageConsumptionControlPort consumption) {
        this.probe = probe;
        this.tracker = tracker;
        this.recordNotificationUseCase = recordNotificationUseCase;
        this.consumption = consumption;
    }

    public void check() {
        switch (tracker.report(probe.isHealthy())) {
            case WENT_DOWN -> {
                log.error("Message broker unavailable: pausing message consumption");
                consumption.pause();
                recordEvent("MessageBrokerUnavailable", "Message broker is unavailable");
            }
            case RECOVERED -> {
                log.info("Message broker recovered: resuming message consumption");
                consumption.resume();
                recordEvent("MessageBrokerRecovered", "Message broker has recovered");
            }
            case NONE -> { }
        }
    }

    public boolean isBrokerUp() {
        return tracker.isUp();
    }

    private void recordEvent(String eventType, String description) {
        Instant now = Instant.now();
        String payload = "{\"eventType\":\"" + eventType + "\",\"description\":\"" + description + "\"}";
        recordNotificationUseCase.record(UUID.randomUUID().toString(), eventType, AGGREGATE, now, payload, null);
    }
}
