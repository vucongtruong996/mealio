package com.mealio.mealio_menu_service.application.usecase;

import java.util.Optional;
import java.util.UUID;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.dto.CreateCategoryResponse;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateCategoryUseCase {
    private final CategoryRepository categoryRepository;
    
    CreateCategoryResponse execute(CreateCategoryCommand command) {
        Optional<Category> existing = categoryRepository.findByName(command.name());
        if(existing.isPresent()) {
            throw new CategoryAlreadyExistsException(command.name());
        }

        Category category = Category.create(command.name(), command.description());
        Category savedCategory = categoryRepository.save(category);

        return new CreateCategoryResponse(savedCategory.getId(), savedCategory.getName(), savedCategory.getDescription());
    }


}
