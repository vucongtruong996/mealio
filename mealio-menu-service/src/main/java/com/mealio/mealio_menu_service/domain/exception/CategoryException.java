package com.mealio.mealio_menu_service.domain.exception;

public abstract class CategoryException extends RuntimeException{
    
    protected CategoryException(String message) {
        super(message);
    }
}
