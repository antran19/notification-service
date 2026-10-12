package com.nexus.notification.infrastructure.config;

import com.nexus.notification.application.usecase.CheckBrokerHealthUseCase;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

// Appears as "brokerHealth" under /actuator/health.
@Component("brokerHealth")
public class BrokerHealthIndicator implements HealthIndicator {

    private final CheckBrokerHealthUseCase checkBrokerHealthUseCase;

    public BrokerHealthIndicator(CheckBrokerHealthUseCase checkBrokerHealthUseCase) {
        this.checkBrokerHealthUseCase = checkBrokerHealthUseCase;
    }

    @Override
    public Health health() {
        return checkBrokerHealthUseCase.isBrokerUp()
                ? Health.up().withDetail("broker", "kafka").build()
                : Health.down().withDetail("broker", "kafka").build();
    }
}
