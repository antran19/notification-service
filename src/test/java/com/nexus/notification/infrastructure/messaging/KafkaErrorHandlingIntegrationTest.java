package com.nexus.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.events.AuctionWonEvent;
import com.nexus.common.kafka.DeadLetterService;
import com.nexus.common.kafka.NexusKafkaAutoConfiguration;
import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.application.usecase.RecordNotificationUseCase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessResourceFailureException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// End-to-end check of common-kafka against a real broker: a record that fails is retried, one
// that keeps failing (or can never be parsed) ends up on this service's own DLT instead of being
// dropped, and DeadLetterService replays it once the cause is fixed.
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class KafkaErrorHandlingIntegrationTest {

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

    private static final String DLT = "auction-events.notification-service.DLT";

    @Autowired private NotificationRepositoryPort repository;
    @Autowired private DeadLetterService deadLetterService;
    @Autowired private ObjectMapper objectMapper;
    @SpyBean private RecordNotificationUseCase recordNotificationUseCase;

    @Test
    void transientFailure_isRetried_andTheEventIsRecordedOnce() throws Exception {
        AuctionWonEvent event = new AuctionWonEvent("auction-t", "product-t", "seller-t", "winner-t", new BigDecimal("10.00"));
        // First attempt: the database "blips"; the retry 1s later goes through for real.
        doThrow(new DataAccessResourceFailureException("db blip")).doCallRealMethod()
                .when(recordNotificationUseCase).record(eq(event.getEventId()), any(), any(), any(), any(), any());

        produce("auction-events", event.getAggregateId(), objectMapper.writeValueAsString(event));

        assertThat(pollNotifications("winner-t", Duration.ofSeconds(15))).hasSize(1);
        verify(recordNotificationUseCase, times(2)).record(eq(event.getEventId()), any(), any(), any(), any(), any());
    }

    @Test
    void persistentFailure_endsUpInDeadLetterTopicAfterRetries() throws Exception {
        AuctionWonEvent event = new AuctionWonEvent("auction-p", "product-p", "seller-p", "winner-p", new BigDecimal("20.00"));
        doThrow(new DataAccessResourceFailureException("db down"))
                .when(recordNotificationUseCase).record(eq(event.getEventId()), any(), any(), any(), any(), any());

        produce("auction-events", event.getAggregateId(), objectMapper.writeValueAsString(event));

        // 1 try + 3 retries waiting 1s + 2s + 4s, so allow well over 7s.
        assertThat(pollDeadLetters(DLT, event.getEventId(), Duration.ofSeconds(30))).isTrue();
        verify(recordNotificationUseCase, times(1 + NexusKafkaAutoConfiguration.MAX_RETRIES))
                .record(eq(event.getEventId()), any(), any(), any(), any(), any());
        assertThat(repository.findByRecipientUserId("winner-p")).isEmpty();
    }

    // The whole operator flow: fails for good -> parked in the DLT -> cause fixed -> replayed ->
    // processed exactly once, and the backlog is back to zero.
    @Test
    void deadLetter_isReplayedOnceTheCauseIsFixed() throws Exception {
        AuctionWonEvent event = new AuctionWonEvent("auction-r", "product-r", "seller-r", "winner-r", new BigDecimal("30.00"));
        doThrow(new DataAccessResourceFailureException("db down"))
                .when(recordNotificationUseCase).record(eq(event.getEventId()), any(), any(), any(), any(), any());
        produce("auction-events", event.getAggregateId(), objectMapper.writeValueAsString(event));
        assertThat(pollDeadLetters(DLT, event.getEventId(), Duration.ofSeconds(30))).isTrue();
        assertThat(repository.findByRecipientUserId("winner-r")).isEmpty();

        doCallRealMethod()
                .when(recordNotificationUseCase).record(eq(event.getEventId()), any(), any(), any(), any(), any());
        replayUntilClear();

        assertThat(repository.findByRecipientUserId("winner-r")).hasSize(1);
        assertThat(deadLetterService.backlog()).containsEntry(DLT, 0L);
    }

    // A record that still can't be processed stops the replay and stays in the DLT.
    @Test
    void replay_stopsAtARecordThatStillFails() {
        String marker = "still-broken-" + UUID.randomUUID();
        produce("auction-events", "bad-key", "{not valid json " + marker);
        assertThat(pollDeadLetters(DLT, marker, Duration.ofSeconds(15))).isTrue();

        DeadLetterService.ReplayResult result = deadLetterService.replay(1000);

        assertThat(result.failure()).isNotNull();
        assertThat(result.failure().deadLetterTopic()).isEqualTo(DLT);
        // Test order is not fixed: the first failing record may be this one or one parked by
        // another test (e.g. the persistent-failure one) -- either way the replay must stop.
        assertThat(result.failure().error()).isNotBlank();
        assertThat(deadLetterService.backlog().get(DLT)).isPositive();
        assertThat(deadLetterService.status(5)).anySatisfy(status -> {
            assertThat(status.deadLetterTopic()).isEqualTo(DLT);
            assertThat(status.oldest()).isNotEmpty();
            assertThat(status.oldest().get(0).error()).isNotBlank();
        });
    }

    @Test
    void malformedPayload_goesStraightToDeadLetterTopic() {
        String marker = "malformed-" + UUID.randomUUID();
        produce("auction-events", "bad-key", "{not valid json " + marker);

        assertThat(pollDeadLetters(DLT, marker, Duration.ofSeconds(15))).isTrue();
    }

    // Other tests in this class park malformed records in the same DLT on purpose; those can
    // never succeed, so step past them (commit offset+1 like a manual "skip") and keep replaying.
    private void replayUntilClear() {
        for (int i = 0; i < 20; i++) {
            DeadLetterService.ReplayResult result = deadLetterService.replay(1000);
            if (result.failure() == null) {
                return;
            }
            skip(result.failure());
        }
    }

    private void skip(DeadLetterService.Failure failure) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-dlt-replay");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            org.apache.kafka.common.TopicPartition tp =
                    new org.apache.kafka.common.TopicPartition(failure.deadLetterTopic(), failure.partition());
            consumer.assign(List.of(tp));
            consumer.commitSync(java.util.Map.of(tp,
                    new org.apache.kafka.clients.consumer.OffsetAndMetadata(failure.offset() + 1)));
        }
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

    private boolean pollDeadLetters(String topic, String containing, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-reader-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.value() != null && record.value().contains(containing)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<?> pollNotifications(String recipientUserId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        List<?> found = new ArrayList<>();
        while (System.currentTimeMillis() < deadline) {
            found = repository.findByRecipientUserId(recipientUserId);
            if (!found.isEmpty()) {
                return found;
            }
            Thread.sleep(200);
        }
        return found;
    }
}
