package com.nexus.notification.application.usecase;

import com.nexus.common.events.UserRegisteredEvent;
import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RecordNotificationUseCaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired private RecordNotificationUseCase recordNotificationUseCase;
    @Autowired private NotificationRepositoryPort repository;

    @Test
    void record_mappedEventType_derivesRecipientAndMessage() {
        UserRegisteredEvent event = new UserRegisteredEvent("user-1", "alice@example.com", "Alice");

        recordNotificationUseCase.record(event.getEventId(), "UserRegistered", "user-1",
                Instant.now(), "{\"eventType\":\"UserRegistered\"}", event);

        assertThat(repository.findByRecipientUserId("user-1")).hasSize(1);
        assertThat(repository.findByRecipientUserId("user-1").get(0).getMessage()).contains("Alice");
    }

    @Test
    void record_unmappedEventType_stillStoresRowWithNullRecipientAndMessage() {
        String eventId = UUID.randomUUID().toString();

        recordNotificationUseCase.record(eventId, "ProductCreated", "product-1",
                Instant.now(), "{\"eventType\":\"ProductCreated\"}", null);

        var all = repository.findAllFiltered("ProductCreated", null);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getRecipientUserId()).isNull();
        assertThat(all.get(0).getMessage()).isNull();
        assertThat(all.get(0).getPayload()).isEqualTo("{\"eventType\":\"ProductCreated\"}");
    }
}
