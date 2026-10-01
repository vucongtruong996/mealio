package com.mealio.mealio_menu_service.domain.exception;

public final class CategoryNotFoundException extends CategoryException {

    public CategoryNotFoundException(String categoryId) {
        super("Category '%s' was not found.".formatted(categoryId));
    }

}