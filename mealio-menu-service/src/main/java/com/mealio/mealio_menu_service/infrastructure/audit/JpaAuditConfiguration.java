package com.mealio.mealio_menu_service.infrastructure.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditConfiguration {
    
    @Bean
    public AuditorAwareImpl auditorAware(CurrentActorProvider currentActorProvider) {
        return new AuditorAwareImpl(currentActorProvider);
    }
}
