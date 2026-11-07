package com.nexus.notification.infrastructure.persistence;

import com.nexus.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {
    List<NotificationJpaEntity> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);
    long countByRecipientUserIdAndReadFalse(String recipientUserId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationJpaEntity n SET n.read = true WHERE n.recipientUserId = :recipientUserId AND n.read = false")
    int markAllRead(@Param("recipientUserId") String recipientUserId);

    List<NotificationJpaEntity> findAllByOrderByCreatedAtDesc();
    List<NotificationJpaEntity> findAllByEventTypeOrderByCreatedAtDesc(String eventType);
    List<NotificationJpaEntity> findAllByAggregateIdOrderByCreatedAtDesc(String aggregateId);
    List<NotificationJpaEntity> findAllByEventTypeAndAggregateIdOrderByCreatedAtDesc(String eventType, String aggregateId);
}
