package com.mealio.mealio_menu_service.domain.exception;

public final class CategoryAlreadyExistsException extends CategoryException {
    
    public CategoryAlreadyExistsException(String categoryName) {
        super("Category '%s' already exists.".formatted(categoryName));
    }
}
