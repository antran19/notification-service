package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.KnownUserEmailRepositoryPort;

public class CacheUserEmailUseCase {

    private final KnownUserEmailRepositoryPort repository;

    public CacheUserEmailUseCase(KnownUserEmailRepositoryPort repository) {
        this.repository = repository;
    }

    public void cache(String userId, String email) {
        repository.upsert(userId, email);
    }
}
