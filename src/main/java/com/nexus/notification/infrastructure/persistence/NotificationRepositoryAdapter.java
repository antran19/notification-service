package com.nexus.notification.infrastructure.persistence;

import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.domain.model.Notification;
import com.nexus.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationRepositoryAdapter.class);

    private final NotificationJpaRepository repository;

    public NotificationRepositoryAdapter(NotificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(Notification notification) {
        NotificationJpaEntity entity = toEntity(notification);
        try {
            // saveAndFlush (not save) so the unique-constraint violation, if any, surfaces
            // here and now -- a plain save() only flushes lazily, which would let a
            // duplicate escape this try/catch and surface somewhere unrelated later.
            repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicateEventId(e)) {
                // A DataIntegrityViolationException can also mean a genuinely malformed
                // row (e.g. a value too long for its column) -- that must not be silently
                // mislabeled and swallowed as "harmless duplicate".
                throw e;
            }
            log.info("Notification for event {} already recorded, skipping duplicate", notification.getEventId());
        }
    }

    private static boolean isDuplicateEventId(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        return cause instanceof java.sql.SQLException sqlEx && "23505".equals(sqlEx.getSQLState());
    }

    @Override
    public void update(Notification notification) {
        repository.save(toEntity(notification));
    }

    @Override
    public List<Notification> findByRecipientUserId(String recipientUserId) {
        return repository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId).stream()
                .map(NotificationRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Notification> findAllFiltered(String eventType, String aggregateId) {
        List<NotificationJpaEntity> entities;
        if (eventType != null && aggregateId != null) {
            entities = repository.findAllByEventTypeAndAggregateIdOrderByCreatedAtDesc(eventType, aggregateId);
        } else if (eventType != null) {
            entities = repository.findAllByEventTypeOrderByCreatedAtDesc(eventType);
        } else if (aggregateId != null) {
            entities = repository.findAllByAggregateIdOrderByCreatedAtDesc(aggregateId);
        } else {
            entities = repository.findAllByOrderByCreatedAtDesc();
        }
        return entities.stream().map(NotificationRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<Notification> findById(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            // A non-UUID id is a not-found, not a server error -- callers (e.g. the
            // mark-read use case) already map an empty Optional to a 404.
            return Optional.empty();
        }
        return repository.findById(uuid).map(NotificationRepositoryAdapter::toDomain);
    }

    private static NotificationJpaEntity toEntity(Notification n) {
        return new NotificationJpaEntity(UUID.fromString(n.getId()), n.getEventId(), n.getEventType(),
                n.getAggregateId(), n.getRecipientUserId(), n.getMessage(), n.getPayload(), n.isRead(),
                n.getOccurredAt(), n.getCreatedAt());
    }

    private static Notification toDomain(NotificationJpaEntity e) {
        return new Notification(e.getId().toString(), e.getEventId(), e.getEventType(), e.getAggregateId(),
                e.getRecipientUserId(), e.getMessage(), e.getPayload(), e.isRead(), e.getOccurredAt(), e.getCreatedAt());
    }
}
