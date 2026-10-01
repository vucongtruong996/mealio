package com.mealio.mealio_menu_service.infrastructure.persistence.mapper;

import java.util.Objects;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.infrastructure.persistence.entity.CategoryEntity;

@Component
public class CategoryPersistenceMapper {

    public @NonNull CategoryEntity toEntity(@NonNull Category category) {
        Objects.requireNonNull(category, "category must not be null");

        return new CategoryEntity(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }

    public @NonNull Category toDomain(@NonNull CategoryEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        UUID id = Objects.requireNonNull(entity.getId(), "entity.id must not be null");
        String name = Objects.requireNonNull(entity.getName(), "entity.name must not be null");
        String description = entity.getDescription();
        String safeDescription = description == null ? "" : description;

        return Category.restore(
                id,
                name,
                safeDescription,
                entity.isActive()
        );
    }

}