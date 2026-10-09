package com.nexus.notification.infrastructure.email;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SmtpEmailSenderAdapterTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    private static MimeMessage blankMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    void send_doesNothingWhenMailUsernameIsBlank() {
        SmtpEmailSenderAdapter adapter = new SmtpEmailSenderAdapter(mailSender, "no-reply@nexus.local", "");

        adapter.send("alice@example.com", "Subject", "Body");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void send_sendsAMimeMessageWhenMailUsernameIsConfigured() throws Exception {
        MimeMessage mimeMessage = blankMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        SmtpEmailSenderAdapter adapter = new SmtpEmailSenderAdapter(mailSender, "no-reply@nexus.local", "smtp-user");

        adapter.send("alice@example.com", "Subject", "Body");

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("Subject");
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("alice@example.com");
    }

    @Test
    void send_doesNotPropagateWhenSendingThrows() {
        MimeMessage mimeMessage = blankMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("boom")).when(mailSender).send(any(MimeMessage.class));
        SmtpEmailSenderAdapter adapter = new SmtpEmailSenderAdapter(mailSender, "no-reply@nexus.local", "smtp-user");

        assertThatCode(() -> adapter.send("alice@example.com", "Subject", "Body")).doesNotThrowAnyException();
    }
}
