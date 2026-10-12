package com.nexus.notification.application.port.out;

// Pauses/resumes everything this service consumes from the message broker.
public interface MessageConsumptionControlPort {
    void pause();
    void resume();
}
