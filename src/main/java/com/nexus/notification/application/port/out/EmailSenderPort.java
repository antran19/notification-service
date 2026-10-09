package com.nexus.notification.application.port.out;

public interface EmailSenderPort {
    void send(String toEmail, String subject, String body);
}
