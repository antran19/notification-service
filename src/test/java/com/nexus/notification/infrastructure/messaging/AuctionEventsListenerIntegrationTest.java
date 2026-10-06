package com.nexus.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.events.AuctionWonEvent;
import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AuctionEventsListenerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_db").withUsername("nexus").withPassword("nexus");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.1"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired private NotificationRepositoryPort repository;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void consumingAuctionWonEvent_recordsNotificationForWinner() throws Exception {
        AuctionWonEvent event = new AuctionWonEvent("auction-1", "product-1", "seller-1", "winner-1", new BigDecimal("500.00"));
        produce("auction-events", event.getAggregateId(), objectMapper.writeValueAsString(event));

        List<com.nexus.notification.domain.model.Notification> found = pollUntilFound("winner-1", Duration.ofSeconds(10));

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getMessage()).contains("500.00");
    }

    @Test
    void consumingMalformedPayload_doesNotCrashListener_andLaterValidMessageStillProcessed() throws Exception {
        produce("auction-events", "bad-key", "{not valid json");

        AuctionWonEvent event = new AuctionWonEvent("auction-2", "product-2", "seller-2", "winner-2", new BigDecimal("77.00"));
        produce("auction-events", event.getAggregateId(), objectMapper.writeValueAsString(event));

        List<com.nexus.notification.domain.model.Notification> found = pollUntilFound("winner-2", Duration.ofSeconds(10));

        assertThat(found).hasSize(1);
    }

    @Test
    void consumingSameEventTwice_viaRealKafkaRedelivery_recordsExactlyOneNotification() throws Exception {
        // Same AuctionWonEvent object serialized and produced twice -> same eventId both
        // times, exercising Kafka's at-least-once redelivery at the full listener
        // pipeline level (not just the repository adapter directly, as the Task 2 test does).
        AuctionWonEvent event = new AuctionWonEvent("auction-3", "product-3", "seller-3", "winner-3", new BigDecimal("42.00"));
        String payload = objectMapper.writeValueAsString(event);

        produce("auction-events", event.getAggregateId(), payload);
        produce("auction-events", event.getAggregateId(), payload);

        List<com.nexus.notification.domain.model.Notification> found = pollUntilFound("winner-3", Duration.ofSeconds(10));
        // Give the (duplicate) second message time to arrive and be rejected before asserting.
        Thread.sleep(2000);
        found = repository.findByRecipientUserId("winner-3");

        assertThat(found).hasSize(1);
    }

    private void produce(String topic, String key, String value) {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(topic, key, value)).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<com.nexus.notification.domain.model.Notification> pollUntilFound(String recipientUserId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            List<com.nexus.notification.domain.model.Notification> found = repository.findByRecipientUserId(recipientUserId);
            if (!found.isEmpty()) {
                return found;
            }
            Thread.sleep(200);
        }
        return repository.findByRecipientUserId(recipientUserId);
    }
}
