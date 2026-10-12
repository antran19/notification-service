package com.nexus.notification.infrastructure.scheduling;

import com.nexus.notification.application.usecase.CheckBrokerHealthUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BrokerHealthJob {

    private final CheckBrokerHealthUseCase checkBrokerHealthUseCase;

    public BrokerHealthJob(CheckBrokerHealthUseCase checkBrokerHealthUseCase) {
        this.checkBrokerHealthUseCase = checkBrokerHealthUseCase;
    }

    @Scheduled(fixedDelayString = "${nexus.health.broker.interval-ms:15000}")
    public void run() {
        checkBrokerHealthUseCase.check();
    }
}
