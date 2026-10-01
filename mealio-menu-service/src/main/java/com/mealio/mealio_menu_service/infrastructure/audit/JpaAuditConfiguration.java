package com.mealio.mealio_menu_service.infrastructure.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@AutoConfiguration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditConfiguration {
    
    @Bean
    public AuditorAwareImpl auditorAware(CurrentActorProvider currentActorProvider) {
        return new AuditorAwareImpl(currentActorProvider);
    }
}
