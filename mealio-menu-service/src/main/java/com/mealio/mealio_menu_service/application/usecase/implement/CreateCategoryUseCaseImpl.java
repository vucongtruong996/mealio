package com.mealio.mealio_menu_service.application.usecase.implement;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.category.CreateCategoryUseCase;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CreateCategoryUseCaseImpl implements CreateCategoryUseCase {
    private final CategoryRepository categoryRepository;
    private final CategoryApplicationMapper categoryApplicationMapper;

    @Override
    public CategoryResult execute(CreateCategoryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(categoryRepository, "categoryRepository must not be null");
        Objects.requireNonNull(categoryApplicationMapper, "categoryApplicationMapper must not be null");

        String categoryName = Objects.requireNonNull(command.name(), "command.name must not be null");
        String categoryDescription = Objects.requireNonNull(command.description(), "command.description must not be null");

        if (categoryRepository.existsByName(categoryName)) {
            throw new CategoryAlreadyExistsException(categoryName);
        }

        Category newCategory = Category.create(categoryName, categoryDescription, true);
        Category savedCategory = Objects.requireNonNull(categoryRepository.save(newCategory), "savedCategory must not be null");

        return categoryApplicationMapper.toResult(savedCategory);
    }
    
}
