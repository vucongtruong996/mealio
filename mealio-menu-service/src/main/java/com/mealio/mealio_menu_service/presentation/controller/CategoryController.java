package com.mealio.mealio_menu_service.presentation.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.CreateCategoryUseCase;
import com.mealio.mealio_menu_service.presentation.mapper.CategoryPresentationMapper;
import com.mealio.mealio_menu_service.presentation.request.CreateCategoryRequest;
import com.mealio.mealio_menu_service.presentation.response.CategoryResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CreateCategoryUseCase createCategoryUseCase;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        CreateCategoryCommand command = CategoryPresentationMapper.toCreateCategoryCommand(request);
        
        CategoryResult response = createCategoryUseCase.execute(command);

        URI location = URI.create("/api/v1/categories/" + response.id());

        return ResponseEntity
            .created(location)
            .body(CategoryPresentationMapper.toCategoryResponse(response));
    }
    
    



}
