package com.mealio.mealio_menu_service.domain.exception;

public final class CategoryNameTooLongException extends CategoryException {

    private static final int MAX_LENGTH = 100;

    public CategoryNameTooLongException() {
        super("Category name must not exceed %d characters.".formatted(MAX_LENGTH));
    }

}