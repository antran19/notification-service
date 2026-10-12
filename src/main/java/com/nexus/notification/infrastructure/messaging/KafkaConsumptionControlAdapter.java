package com.nexus.notification.infrastructure.messaging;

import com.nexus.notification.application.port.out.MessageConsumptionControlPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumptionControlAdapter implements MessageConsumptionControlPort {

    // ObjectProvider: slice tests build a context without Kafka's listener registry.
    private final ObjectProvider<KafkaListenerEndpointRegistry> registry;

    public KafkaConsumptionControlAdapter(ObjectProvider<KafkaListenerEndpointRegistry> registry) {
        this.registry = registry;
    }

    @Override
    public void pause() {
        registry.ifAvailable(r -> r.getListenerContainers().forEach(MessageListenerContainer::pause));
    }

    @Override
    public void resume() {
        registry.ifAvailable(r -> r.getListenerContainers().forEach(MessageListenerContainer::resume));
    }
}
