package com.mealio.mealio_menu_service.infrastructure.audit;

import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class AuditorAwareImpl implements AuditorAware<String> {

    private final CurrentActorProvider currentActorProvider;

    public AuditorAwareImpl(CurrentActorProvider currentActorProvider) {
        this.currentActorProvider = Objects.requireNonNull(currentActorProvider, "currentActorProvider must not be null");
    }

    @Override
    public @NonNull Optional<String> getCurrentAuditor() {
        String currentActor = currentActorProvider.getCurrentActor();
        if (currentActor == null) {
            return Optional.<String>empty();
        }

        String trimmedActor = currentActor.trim();
        if (trimmedActor.isEmpty()) {
            return Optional.<String>empty();
        }

        return Optional.of(trimmedActor);
    }

    @Nullable
    public String getCurrentActor() {
        return currentActorProvider.getCurrentActor();
    }

}
