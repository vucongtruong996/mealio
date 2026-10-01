package com.mealio.mealio_menu_service.presentation.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.category.BulkCreateCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.CreateCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.ListCategoriesUseCase;
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
    private final BulkCreateCategoryUseCase bulkCreateCategoryUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody @NonNull CreateCategoryRequest request) {
        CreateCategoryCommand command = CategoryPresentationMapper.toCreateCategoryCommand(request);

        CategoryResult response = createCategoryUseCase.execute(command);

        URI location = URI.create("/api/v1/categories/" + response.id());

        return ResponseEntity
            .created(location)
            .body(CategoryPresentationMapper.toCategoryResponse(response));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<CategoryResponse>> createCategories(
            @Valid @RequestBody @NonNull List<CreateCategoryRequest> requests) {
        List<CreateCategoryCommand> commands = requests.stream()
                .map(CategoryPresentationMapper::toCreateCategoryCommand)
                .toList();
            
        List<CategoryResult> results = bulkCreateCategoryUseCase.executeBulk(commands);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                results.stream()
                        .map(CategoryPresentationMapper::toCategoryResponse)
                        .toList()
        );  
    }
    
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(Pageable pageable) {
        List<CategoryResponse> responses = listCategoriesUseCase.listCategories(pageable)
                .getContent()
                .stream()
                .map(CategoryPresentationMapper::toCategoryResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable UUID id) {
        // Implement the logic to retrieve the category by ID
        // For example, you can call a use case to get the category and return the response
        
        return ResponseEntity.ok().build(); // Placeholder response 
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID id
        , @Valid @RequestBody CreateCategoryRequest request) {
        // Implement the logic to update the category by ID
        // For example, you can call a use case to update the category and return the response
        
        return ResponseEntity.ok().build(); // Placeholder response 
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        // Implement the logic to delete the category by ID
        // For example, you can call a use case to delete the category  

        
        return ResponseEntity.noContent().build(); // Placeholder response
    }

}
