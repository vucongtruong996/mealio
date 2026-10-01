package com.mealio.mealio_menu_service.domain.model;

import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

import org.springframework.lang.NonNull;

import com.mealio.mealio_menu_service.domain.exception.CategoryDescriptionTooLongException;
import com.mealio.mealio_menu_service.domain.exception.CategoryInactiveException;
import com.mealio.mealio_menu_service.domain.exception.CategoryNameCannotBeBlankException;
import com.mealio.mealio_menu_service.domain.exception.CategoryNameTooLongException;

@Getter
public final class Category {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final UUID id;

    private String name;

    private String description;

    private boolean active;
    private Category(
            UUID id,
            String name,
            String description,
            boolean active
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.active = active;
    }

    /**
     * Create a brand-new Category.
     */
    public static @NonNull Category create(
            @NonNull String name,
            @NonNull String description,
            boolean active
    ) {
        return new Category(
                UUID.randomUUID(),
                name,
                description,
                active
        );
    }

    /**
     * Restore an existing Category from persistence.
     */
    public static @NonNull Category restore(
            @NonNull UUID id,
            @NonNull String name,
            @NonNull String description,
            boolean active
    ) {

        return new Category(
                id,
                name,
                description,
                active
        );
    }

    public void rename(
            @NonNull String newName
    ) {

        this.name = normalizeName(newName);
    }

    public void changeDescription(
            @NonNull String description
    ) {

        this.description = normalizeDescription(description);
    }

    public void activate() {

        if (!active) {
            this.active = true;
        }
    }

    public void deactivate(String updatedBy) {

        if (active) {
            this.active = false;
        }
    }

    public void validateActive() {

        if (!active) {
            throw new CategoryInactiveException(name);
        }
    }

    private static String normalizeName(String value) {

        if (value == null || value.isBlank()) {
            throw new CategoryNameCannotBeBlankException();
        }

        String normalized = value.trim();

        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new CategoryNameTooLongException();
        }

        return normalized;
    }

    private static String normalizeDescription(String value) {

        if (value == null || value.isBlank()) {
            throw new CategoryNameCannotBeBlankException();
        }

        String normalized = value.trim();

        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new CategoryDescriptionTooLongException();
        }

        return normalized;
    }

}