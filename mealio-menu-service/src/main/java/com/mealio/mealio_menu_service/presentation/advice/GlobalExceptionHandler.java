package com.mealio.mealio_menu_service.presentation.advice;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.exception.CategoryNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFoundException(CategoryNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Category Not Found");
        problemDetail.setType(URI.create("errors/category-not-found"));
        problemDetail.setProperty("code", "CATEGORY_NOT_FOUND");
        return problemDetail;
    }
    
    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ProblemDetail handleCategoryAlreadyExistsException(CategoryAlreadyExistsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Category Already Exists");
        problemDetail.setDetail(ex.getMessage().replace("'", ""));
        problemDetail.setType(URI.create("errors/category-already-exists"));
        return problemDetail;
    }


}
