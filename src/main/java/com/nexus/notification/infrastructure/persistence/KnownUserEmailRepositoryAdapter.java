package com.nexus.notification.infrastructure.persistence;

import com.nexus.notification.application.port.out.KnownUserEmailRepositoryPort;
import com.nexus.notification.infrastructure.persistence.entity.KnownUserEmailJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class KnownUserEmailRepositoryAdapter implements KnownUserEmailRepositoryPort {

    private final KnownUserEmailJpaRepository jpaRepository;

    public KnownUserEmailRepositoryAdapter(KnownUserEmailJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void upsert(String userId, String email) {
        jpaRepository.save(new KnownUserEmailJpaEntity(userId, email));
    }

    @Override
    public Optional<String> findEmailByUserId(String userId) {
        return jpaRepository.findById(userId).map(KnownUserEmailJpaEntity::getEmail);
    }
}
