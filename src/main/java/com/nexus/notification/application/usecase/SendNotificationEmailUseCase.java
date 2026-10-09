package com.nexus.notification.application.usecase;

import com.nexus.notification.application.port.out.EmailSenderPort;
import com.nexus.notification.application.port.out.KnownUserEmailRepositoryPort;

public class SendNotificationEmailUseCase {

    private static final String SUBJECT = "Thông báo từ Nexus";

    private final KnownUserEmailRepositoryPort knownUserEmailRepositoryPort;
    private final EmailSenderPort emailSenderPort;

    public SendNotificationEmailUseCase(KnownUserEmailRepositoryPort knownUserEmailRepositoryPort,
                                         EmailSenderPort emailSenderPort) {
        this.knownUserEmailRepositoryPort = knownUserEmailRepositoryPort;
        this.emailSenderPort = emailSenderPort;
    }

    // Silently does nothing when no email is on file for this recipient (e.g. they
    // registered before this cache existed) -- same fail-open convention used elsewhere
    // in this codebase for "optional, best-effort" side effects.
    public void send(String recipientUserId, String message) {
        knownUserEmailRepositoryPort.findEmailByUserId(recipientUserId)
                .ifPresent(email -> emailSenderPort.send(email, SUBJECT, message));
    }
}
