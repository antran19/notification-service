package com.nexus.notification.infrastructure.config;

import com.nexus.notification.application.port.out.EmailSenderPort;
import com.nexus.notification.application.port.out.KnownUserEmailRepositoryPort;
import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.application.service.NotificationMessageResolver;
import com.nexus.notification.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public NotificationMessageResolver notificationMessageResolver() {
        return new NotificationMessageResolver();
    }

    @Bean
    public RecordNotificationUseCase recordNotificationUseCase(NotificationRepositoryPort repository,
                                                                  NotificationMessageResolver resolver) {
        return new RecordNotificationUseCase(repository, resolver);
    }

    @Bean
    public CacheUserEmailUseCase cacheUserEmailUseCase(KnownUserEmailRepositoryPort repository) {
        return new CacheUserEmailUseCase(repository);
    }

    @Bean
    public SendNotificationEmailUseCase sendNotificationEmailUseCase(KnownUserEmailRepositoryPort repository,
                                                                        EmailSenderPort emailSenderPort) {
        return new SendNotificationEmailUseCase(repository, emailSenderPort);
    }

    @Bean
    public ListMyNotificationsUseCase listMyNotificationsUseCase(NotificationRepositoryPort repository) {
        return new ListMyNotificationsUseCase(repository);
    }

    @Bean
    public ListAllNotificationsUseCase listAllNotificationsUseCase(NotificationRepositoryPort repository) {
        return new ListAllNotificationsUseCase(repository);
    }

    @Bean
    public MarkNotificationReadUseCase markNotificationReadUseCase(NotificationRepositoryPort repository) {
        return new MarkNotificationReadUseCase(repository);
    }
}
