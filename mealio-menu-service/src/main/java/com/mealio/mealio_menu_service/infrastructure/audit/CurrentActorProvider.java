package com.mealio.mealio_menu_service.infrastructure.audit;

import org.springframework.lang.Nullable;

public interface CurrentActorProvider {

    @Nullable
    String getCurrentActor();
}
