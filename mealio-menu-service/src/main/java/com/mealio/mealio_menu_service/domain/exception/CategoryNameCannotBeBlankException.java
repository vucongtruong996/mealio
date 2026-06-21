package com.mealio.mealio_menu_service.domain.exception;

public class CategoryNameCannotBeBlankException extends IllegalArgumentException {
    public CategoryNameCannotBeBlankException() {
        super("Category name must not be blank");
    }
    
}
