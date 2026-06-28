package com.mealio.mealio_menu_service.presentation.advice;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ProblemDetail handleCategoryAlreadyExistsException(CategoryAlreadyExistsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Category Already Exists");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setType(URI.create("errors/category-already-exists"));
        return problemDetail;
    }


}
