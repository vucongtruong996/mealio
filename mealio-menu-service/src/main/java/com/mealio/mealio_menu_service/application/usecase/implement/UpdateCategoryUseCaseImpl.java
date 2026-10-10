package com.mealio.mealio_menu_service.application.usecase.implement;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealio.mealio_menu_service.application.command.UpdateCategoryCommand;
import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.category.UpdateCategoryUseCase;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.exception.CategoryNotFoundException;
import com.mealio.mealio_menu_service.domain.model.Category;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateCategoryUseCaseImpl implements UpdateCategoryUseCase {
    private final CategoryRepository categoryRepository;
    private final CategoryApplicationMapper categoryApplicationMapper;

    @Override
    public CategoryResult execute(UUID id, UpdateCategoryCommand command) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(command, "command must not be null");

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));

        category.rename(command.name());
        category.changeDescription(command.description());

        categoryRepository.findByName(category.getName())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new CategoryAlreadyExistsException(category.getName());
                });

        return categoryApplicationMapper.toResult(categoryRepository.save(category));
    }
}
