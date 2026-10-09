package com.nexus.notification.infrastructure.persistence;

import com.nexus.notification.infrastructure.persistence.entity.KnownUserEmailJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnownUserEmailJpaRepository extends JpaRepository<KnownUserEmailJpaEntity, String> {
}
