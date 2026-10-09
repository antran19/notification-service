package com.nexus.notification.application.port.out;

import java.util.Optional;

public interface KnownUserEmailRepositoryPort {
    void upsert(String userId, String email);
    Optional<String> findEmailByUserId(String userId);
}
