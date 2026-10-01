package com.mealio.mealio_menu_service.domain.exception;

public final class CategoryDescriptionTooLongException extends CategoryException {

    private static final int MAX_LENGTH = 500;

    public CategoryDescriptionTooLongException() {
        super("Category description must not exceed %d characters.".formatted(MAX_LENGTH));
    }

}