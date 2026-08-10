package com.jclinical.notifications.infra.config;

import com.jclinical.notifications.domain.ports.out.NotificationRepositoryPort;
import com.jclinical.notifications.domain.service.NotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationDomainConfig {

    @Bean
    public NotificationService notificationService(NotificationRepositoryPort repository) {
        return new NotificationService(repository);
    }
}
