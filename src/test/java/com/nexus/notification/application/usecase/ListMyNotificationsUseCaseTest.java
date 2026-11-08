package com.nexus.notification.application.usecase;

import com.nexus.common.core.exception.ValidationException;
import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.domain.model.Notification;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListMyNotificationsUseCaseTest {

    private final NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);
    private final ListMyNotificationsUseCase useCase = new ListMyNotificationsUseCase(repository);

    @Test
    void asksTheRepositoryForTheGivenRecipientFilterAndPage() {
        Notification n = Notification.record("e-1", "UserRegistered", "user-1", "user-1", "Hi", "{}", Instant.now());
        when(repository.findPageByRecipient("user-1", true, 1, 10)).thenReturn(List.of(n));

        assertThat(useCase.list("user-1", true, 1, 10)).containsExactly(n);
    }

    @Test
    void theBoundsOfSizeAreAccepted() {
        useCase.list("user-1", false, 0, 1);
        useCase.list("user-1", false, 0, 100);

        verify(repository).findPageByRecipient("user-1", false, 0, 1);
        verify(repository).findPageByRecipient("user-1", false, 0, 100);
    }

    @Test
    void aBadPageOrSize_isRejectedBeforeTheDatabaseIsTouched() {
        assertThatThrownBy(() -> useCase.list("user-1", false, -1, 20)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.list("user-1", false, 0, 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.list("user-1", false, 0, 101)).isInstanceOf(ValidationException.class);
        verify(repository, never()).findPageByRecipient(anyString(), anyBoolean(), anyInt(), anyInt());
    }
}
