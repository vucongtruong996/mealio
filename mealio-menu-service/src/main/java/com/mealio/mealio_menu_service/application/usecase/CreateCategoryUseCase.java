package com.mealio.mealio_menu_service.application.usecase;

import org.springframework.stereotype.Service;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CreateCategoryUseCase {
    private final CategoryRepository categoryRepository;

    public CategoryResult execute(CreateCategoryCommand command) {
        // Check if a category with the same name already exists
        categoryRepository.findByName(command.name())
            .ifPresent(existingCategory -> {
                throw new CategoryAlreadyExistsException(command.name());
            });

        // Create a new category
        Category newCategory = Category.create(command.name(), command.description());

        Category savedCategory = categoryRepository.save(newCategory);

        return new CategoryResult(
            savedCategory.getId(),
            savedCategory.getName(),
            savedCategory.getDescription()
        );
    }



}