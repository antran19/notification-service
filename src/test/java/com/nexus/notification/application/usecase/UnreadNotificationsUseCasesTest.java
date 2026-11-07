package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnreadNotificationsUseCasesTest {

    private final NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);

    @Test
    void count_asksForTheGivenRecipientOnly() {
        when(repository.countUnreadByRecipient("user-1")).thenReturn(5L);

        assertThat(new CountMyUnreadNotificationsUseCase(repository).count("user-1")).isEqualTo(5);
        verify(repository).countUnreadByRecipient("user-1");
    }

    @Test
    void markAllRead_returnsHowManyWereUnread_forTheGivenRecipientOnly() {
        when(repository.markAllReadByRecipient("user-1")).thenReturn(2);

        assertThat(new MarkAllNotificationsReadUseCase(repository).markAllRead("user-1")).isEqualTo(2);
        verify(repository).markAllReadByRecipient("user-1");
    }
}
