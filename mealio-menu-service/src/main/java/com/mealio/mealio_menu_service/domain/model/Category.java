package com.mealio.mealio_menu_service.domain.model;

import java.util.UUID;

import com.mealio.mealio_menu_service.domain.exception.CategoryNameCannotBeBlankException;

public class Category {

    private final UUID id;
    private final String name;
    private final String description;

    private Category(
        UUID id,
        String name,
        String description) {

        if (name == null || name.isBlank()) {
            throw new CategoryNameCannotBeBlankException();
        }

        this.id = id;
        this.name = name;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static Category create(String name, String description) {
        return new Category(UUID.randomUUID(), name, description);
    }

    public static Category restore(UUID id, String name, String description) {
        return new Category(id, name, description);
    }
}