package com.nexus.notification.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaBrokerHealthProbeTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.1"));

    @Test
    void reportsHealthyForAReachableBroker() {
        assertThat(new KafkaBrokerHealthProbe(kafka.getBootstrapServers(), 5000).isHealthy()).isTrue();
    }

    @Test
    void reportsUnhealthyForAnUnreachableBroker() {
        assertThat(new KafkaBrokerHealthProbe("localhost:1", 1500).isHealthy()).isFalse();
    }
}
