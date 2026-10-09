package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.EmailSenderPort;
import com.nexus.notification.application.port.out.KnownUserEmailRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SendNotificationEmailUseCaseTest {

    private final KnownUserEmailRepositoryPort knownUserEmailRepositoryPort = mock(KnownUserEmailRepositoryPort.class);
    private final EmailSenderPort emailSenderPort = mock(EmailSenderPort.class);
    private final SendNotificationEmailUseCase useCase =
            new SendNotificationEmailUseCase(knownUserEmailRepositoryPort, emailSenderPort);

    @Test
    void send_emailsTheKnownAddressForThatRecipient() {
        when(knownUserEmailRepositoryPort.findEmailByUserId("user-1")).thenReturn(Optional.of("alice@example.com"));

        useCase.send("user-1", "You won the auction!");

        verify(emailSenderPort).send(eq("alice@example.com"), any(), eq("You won the auction!"));
    }

    @Test
    void send_doesNothingWhenNoEmailIsKnownForThatRecipient() {
        when(knownUserEmailRepositoryPort.findEmailByUserId("user-2")).thenReturn(Optional.empty());

        useCase.send("user-2", "You won the auction!");

        verify(emailSenderPort, never()).send(any(), any(), any());
    }
}
