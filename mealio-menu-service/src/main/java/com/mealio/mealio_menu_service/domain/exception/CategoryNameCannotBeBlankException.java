package com.mealio.mealio_menu_service.domain.exception;

public final class CategoryNameCannotBeBlankException extends CategoryException {
    
    public CategoryNameCannotBeBlankException() {
        super("Category name cannot be blank.");
    }
    
}
