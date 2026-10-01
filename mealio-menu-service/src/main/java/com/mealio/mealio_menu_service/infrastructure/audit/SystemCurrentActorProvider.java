package com.mealio.mealio_menu_service.infrastructure.audit;

public class SystemCurrentActorProvider implements CurrentActorProvider {

    @Override
    public String getCurrentActor() {
        return "SYSTEM";
    }
    
}
