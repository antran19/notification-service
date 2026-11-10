package com.nexus.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.events.AuctionPaymentTimeoutEvent;
import com.nexus.common.events.OrderPaidEvent;
import com.nexus.common.events.OrderShippedEvent;
import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.domain.model.Notification;
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
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// The order and payment-timeout events, from the producer's JSON to a stored notification, on real Kafka and PostgreSQL.
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CommerceEventsListenerIntegrationTest {

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
    void anOrderPaidEvent_notifiesTheSeller_andOnlyTheSeller() throws Exception {
        OrderPaidEvent event = new OrderPaidEvent("order-p1", "auction-p1", "buyer-p1", "seller-p1",
                new BigDecimal("500.00"), Instant.now());
        produce("commerce-events", event.getAggregateId(), objectMapper.writeValueAsString(event));

        List<Notification> forSeller = pollUntilFound("seller-p1");

        assertThat(forSeller).hasSize(1);
        assertThat(forSeller.get(0).getMessage()).contains("order-p1").contains("500.00");
        assertThat(repository.findByRecipientUserId("buyer-p1")).isEmpty();
    }

    @Test
    void anOrderShippedEvent_notifiesTheBuyer_andOnlyTheBuyer() throws Exception {
        OrderShippedEvent event = new OrderShippedEvent("order-s1", "auction-s1", "buyer-s1", "seller-s1", Instant.now());
        produce("commerce-events", event.getAggregateId(), objectMapper.writeValueAsString(event));

        List<Notification> forBuyer = pollUntilFound("buyer-s1");

        assertThat(forBuyer).hasSize(1);
        assertThat(forBuyer.get(0).getMessage()).contains("order-s1");
        assertThat(repository.findByRecipientUserId("seller-s1")).isEmpty();
    }

    @Test
    void aPaymentTimeoutOnTheAuctionTopic_notifiesTheWinner() throws Exception {
        AuctionPaymentTimeoutEvent event = new AuctionPaymentTimeoutEvent("auction-t1", "winner-t1", "product-t1");
        produce("auction-events", event.getAggregateId(), objectMapper.writeValueAsString(event));

        List<Notification> forWinner = pollUntilFound("winner-t1");

        assertThat(forWinner).hasSize(1);
        assertThat(forWinner.get(0).getMessage()).contains("auction-t1");
    }

    private void produce(String topic, String key, String value) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, key, value)).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Notification> pollUntilFound(String recipientUserId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
        while (System.currentTimeMillis() < deadline) {
            List<Notification> found = repository.findByRecipientUserId(recipientUserId);
            if (!found.isEmpty()) {
                return found;
            }
            Thread.sleep(200);
        }
        return repository.findByRecipientUserId(recipientUserId);
    }
}
