package com.mealio.mealio_menu_service.application.command;

public record CreateCategoryCommand(
        String name,
        String description
    ) {
}