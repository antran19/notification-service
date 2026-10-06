package com.nexus.notification.infrastructure.persistence;

import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.domain.model.Notification;
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
class NotificationRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.kafka.bootstrap-servers", () -> "");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired
    private NotificationRepositoryPort repository;

    @Test
    void record_sameEventIdTwice_resultsInExactlyOneRow() {
        String eventId = UUID.randomUUID().toString();
        Notification first = Notification.record(eventId, "UserRegistered", "user-1",
                "user-1", "Welcome!", "{}", Instant.now());
        Notification duplicate = Notification.record(eventId, "UserRegistered", "user-1",
                "user-1", "Welcome!", "{}", Instant.now());

        repository.record(first);
        repository.record(duplicate);

        assertThat(repository.findByRecipientUserId("user-1")).hasSize(1);
    }

    @Test
    void findAllFiltered_byEventTypeAndAggregateId_returnsOnlyMatchingRows() {
        repository.record(Notification.record(UUID.randomUUID().toString(), "AuctionWon", "auction-1",
                "winner-1", "You won!", "{}", Instant.now()));
        repository.record(Notification.record(UUID.randomUUID().toString(), "ProductCreated", "product-1",
                null, null, "{}", Instant.now()));

        assertThat(repository.findAllFiltered("AuctionWon", null)).hasSize(1);
        assertThat(repository.findAllFiltered(null, "product-1")).hasSize(1);
        assertThat(repository.findAllFiltered(null, null)).hasSize(2);
    }

    @Test
    void findById_withNonUuidString_returnsEmptyInsteadOfThrowing() {
        assertThat(repository.findById("not-a-uuid")).isEmpty();
    }

    @Test
    void record_otherConstraintViolation_isNotSwallowedAsDuplicate_andPropagates() {
        Notification aggregateIdTooLong = Notification.record(UUID.randomUUID().toString(), "X",
                "a".repeat(300), null, null, "{}", Instant.now());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.record(aggregateIdTooLong))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
