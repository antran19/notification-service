package com.nexus.notification.infrastructure.persistence;

import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.domain.model.Notification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void record_sameEventIdTwice_resultsInExactlyOneRow() {
        String eventId = UUID.randomUUID().toString();
        Notification first = Notification.record(eventId, "UserRegistered", "user-1",
                "user-1", "Welcome!", "{}", Instant.now());
        Notification duplicate = Notification.record(eventId, "UserRegistered", "user-1",
                "user-1", "Welcome!", "{}", Instant.now());

        boolean firstStored = repository.record(first);
        boolean duplicateStored = repository.record(duplicate);

        assertThat(firstStored).isTrue();
        assertThat(duplicateStored).isFalse();
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

    // The filter test above counts every row in the table, so the rows made here are removed again.
    @AfterEach
    void removeRowsOfTheUnreadTests() {
        jdbcTemplate.update("DELETE FROM notifications WHERE recipient_user_id LIKE 'unread-%' OR recipient_user_id LIKE 'readall-%' OR recipient_user_id LIKE 'paged-%'");
    }

    private void store(String recipient) {
        repository.record(Notification.record(UUID.randomUUID().toString(), "UserRegistered", recipient,
                recipient, "Hello", "{}", Instant.now()));
    }

    @Test
    void unreadCount_countsOnlyTheRecipientsUnreadRows() {
        String me = "unread-" + UUID.randomUUID();
        String other = "unread-" + UUID.randomUUID();
        store(me);
        store(me);
        store(other);
        Notification read = repository.findByRecipientUserId(me).get(0);
        read.markRead();
        repository.update(read);

        assertThat(repository.countUnreadByRecipient(me)).isEqualTo(1);
        assertThat(repository.countUnreadByRecipient(other)).isEqualTo(1);
        assertThat(repository.countUnreadByRecipient("nobody-" + UUID.randomUUID())).isZero();
    }

    @Test
    void markAllRead_changesOnlyTheRecipientsUnreadRows_andIsRepeatable() {
        String me = "readall-" + UUID.randomUUID();
        String other = "readall-" + UUID.randomUUID();
        store(me);
        store(me);
        store(me);
        store(other);

        assertThat(repository.markAllReadByRecipient(me)).isEqualTo(3);

        assertThat(repository.countUnreadByRecipient(me)).isZero();
        assertThat(repository.findByRecipientUserId(me)).allMatch(Notification::isRead);
        assertThat(repository.countUnreadByRecipient(other)).isEqualTo(1);
        assertThat(repository.markAllReadByRecipient(me)).isZero();
    }

    @Test
    void findPageByRecipient_isNewestFirst_pagedWithoutRepeatsOrGaps_andOnlyTheRecipients() {
        String me = "paged-" + UUID.randomUUID();
        String other = "paged-" + UUID.randomUUID();
        Instant base = Instant.parse("2030-01-01T00:00:00Z");
        for (int i = 0; i < 5; i++) {
            repository.record(Notification.record("paged-e-" + UUID.randomUUID(), "UserRegistered", me, me,
                    "n" + i, "{}", base.plusSeconds(i)));
        }
        store(other);

        List<Notification> first = repository.findPageByRecipient(me, false, 0, 2);
        List<Notification> second = repository.findPageByRecipient(me, false, 1, 2);
        List<Notification> third = repository.findPageByRecipient(me, false, 2, 2);

        assertThat(first).extracting(Notification::getMessage).containsExactly("n4", "n3");
        assertThat(second).extracting(Notification::getMessage).containsExactly("n2", "n1");
        assertThat(third).extracting(Notification::getMessage).containsExactly("n0");
        assertThat(repository.findPageByRecipient(me, false, 3, 2)).isEmpty();
    }

    @Test
    void findPageByRecipient_unreadOnly_leavesOutTheReadOnes() {
        String me = "paged-" + UUID.randomUUID();
        store(me);
        store(me);
        Notification read = repository.findByRecipientUserId(me).get(0);
        read.markRead();
        repository.update(read);

        assertThat(repository.findPageByRecipient(me, true, 0, 10)).hasSize(1)
                .noneMatch(n -> n.getId().equals(read.getId()));
        assertThat(repository.findPageByRecipient(me, false, 0, 10)).hasSize(2);
    }
}
