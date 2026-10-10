package com.mealio.mealio_menu_service.application.usecase.implement;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
public class CreateCategoryUseCaseImpl implements CreateCategoryUseCase {
    private final CategoryRepository categoryRepository;
    private final CategoryApplicationMapper categoryApplicationMapper;

    @Override
    public CategoryResult execute(CreateCategoryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Category newCategory = Category.create(command.name(), command.description(), true);
        if (categoryRepository.existsByName(newCategory.getName())) {
            throw new CategoryAlreadyExistsException(newCategory.getName());
        }

        Category savedCategory = Objects.requireNonNull(categoryRepository.save(newCategory), "savedCategory must not be null");

        return categoryApplicationMapper.toResult(savedCategory);
    }
    
}
