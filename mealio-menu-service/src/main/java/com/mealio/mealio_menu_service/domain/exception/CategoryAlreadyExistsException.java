package com.mealio.mealio_menu_service.domain.exception;

public class CategoryAlreadyExistsException extends RuntimeException {
    public CategoryAlreadyExistsException(String message) {
        super("Category already exists: " + message);
    }
    
}
