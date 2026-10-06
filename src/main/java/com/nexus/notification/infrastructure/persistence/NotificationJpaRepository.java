package com.nexus.notification.infrastructure.persistence;

import com.nexus.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {
    List<NotificationJpaEntity> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);
    List<NotificationJpaEntity> findAllByOrderByCreatedAtDesc();
    List<NotificationJpaEntity> findAllByEventTypeOrderByCreatedAtDesc(String eventType);
    List<NotificationJpaEntity> findAllByAggregateIdOrderByCreatedAtDesc(String aggregateId);
    List<NotificationJpaEntity> findAllByEventTypeAndAggregateIdOrderByCreatedAtDesc(String eventType, String aggregateId);
}
