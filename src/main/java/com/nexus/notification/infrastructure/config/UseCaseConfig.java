package com.nexus.notification.infrastructure.config;

import com.nexus.notification.application.port.out.NotificationRepositoryPort;
import com.nexus.notification.application.service.NotificationMessageResolver;
import com.nexus.notification.application.usecase.RecordNotificationUseCase;
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
}
