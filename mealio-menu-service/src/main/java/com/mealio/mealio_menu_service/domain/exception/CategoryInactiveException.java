package com.mealio.mealio_menu_service.domain.exception;

public final class CategoryInactiveException extends CategoryException {

    public CategoryInactiveException() {
        super("Category is inactive.");
    }

    public CategoryInactiveException(String categoryName) {
        super("Category '%s' is inactive.".formatted(categoryName));
    }

}