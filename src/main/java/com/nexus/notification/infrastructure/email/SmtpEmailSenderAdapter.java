package com.nexus.notification.infrastructure.email;

import com.nexus.notification.application.port.out.EmailSenderPort;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

// Real SMTP delivery via Spring's JavaMailSender. With no spring.mail.username configured
// (true on this machine right now -- no SMTP account has been provisioned for the project
// yet), it logs what would have been sent instead of attempting a connection that can only
// fail; once real credentials are supplied via env vars, this starts actually delivering
// without any code change.
@Component
public class SmtpEmailSenderAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSenderAdapter.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean enabled;

    public SmtpEmailSenderAdapter(JavaMailSender mailSender,
                                   @Value("${nexus.mail.from:no-reply@nexus.local}") String fromAddress,
                                   @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.enabled = mailUsername != null && !mailUsername.isBlank();
    }

    @Override
    public void send(String toEmail, String subject, String body) {
        if (!enabled) {
            log.info("Email channel not configured (no spring.mail.username) -- would have sent to {}: [{}] {}",
                    toEmail, subject, body);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            // Never let an SMTP failure (bad credentials, provider downtime) break the
            // Kafka listener thread that triggered it -- same resilience convention as
            // EventRecordingService's parse-failure handling.
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
