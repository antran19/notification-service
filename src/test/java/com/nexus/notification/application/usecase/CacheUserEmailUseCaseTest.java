package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.KnownUserEmailRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CacheUserEmailUseCaseTest {

    private final KnownUserEmailRepositoryPort repository = mock(KnownUserEmailRepositoryPort.class);
    private final CacheUserEmailUseCase useCase = new CacheUserEmailUseCase(repository);

    @Test
    void cache_upsertsTheUserIdAndEmail() {
        useCase.cache("user-1", "alice@example.com");

        verify(repository).upsert("user-1", "alice@example.com");
    }
}
