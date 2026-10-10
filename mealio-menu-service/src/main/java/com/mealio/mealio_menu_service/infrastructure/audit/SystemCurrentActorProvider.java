package com.mealio.mealio_menu_service.infrastructure.audit;

import org.springframework.stereotype.Component;

@Component
public class SystemCurrentActorProvider implements CurrentActorProvider {

    @Override
    public String getCurrentActor() {
        return "SYSTEM";
    }
    
}
