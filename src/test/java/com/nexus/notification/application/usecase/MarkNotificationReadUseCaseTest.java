package com.nexus.notification.application.usecase;

import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.common.core.exception.NotFoundException;
import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.domain.model.Notification;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarkNotificationReadUseCaseTest {

    private final NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);
    private final MarkNotificationReadUseCase useCase = new MarkNotificationReadUseCase(repository);

    @Test
    void markRead_callerDoesNotOwnNotification_throwsForbidden_andNeverUpdates() {
        Notification ownedByUserA = new Notification("n-1", "e-1", "AuctionWon", "auction-1",
                "user-A", "You won!", "{}", false, Instant.now(), Instant.now());
        when(repository.findById("n-1")).thenReturn(Optional.of(ownedByUserA));

        assertThatThrownBy(() -> useCase.markRead("n-1", "user-B"))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).update(any());
    }

    @Test
    void markRead_notificationHasNullRecipient_throwsForbidden() {
        Notification auditOnly = new Notification("n-2", "e-2", "ProductCreated", "product-1",
                null, null, "{}", false, Instant.now(), Instant.now());
        when(repository.findById("n-2")).thenReturn(Optional.of(auditOnly));

        assertThatThrownBy(() -> useCase.markRead("n-2", "user-A"))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).update(any());
    }

    @Test
    void markRead_callerOwnsNotification_marksReadAndUpdates() {
        Notification ownedByUserA = new Notification("n-3", "e-3", "AuctionWon", "auction-1",
                "user-A", "You won!", "{}", false, Instant.now(), Instant.now());
        when(repository.findById("n-3")).thenReturn(Optional.of(ownedByUserA));

        useCase.markRead("n-3", "user-A");

        verify(repository).update(argThat(Notification::isRead));
    }

    @Test
    void markRead_notificationNotFound_throwsNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.markRead("missing", "user-A"))
                .isInstanceOf(NotFoundException.class);
    }
}
