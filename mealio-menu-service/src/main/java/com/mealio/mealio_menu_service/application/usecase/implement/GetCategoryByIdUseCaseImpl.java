package com.mealio.mealio_menu_service.application.usecase.implement;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.category.GetCategoryByIdUseCase;
import com.mealio.mealio_menu_service.domain.exception.CategoryNotFoundException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCategoryByIdUseCaseImpl implements GetCategoryByIdUseCase {

    private final CategoryRepository categoryRepository;
    private final CategoryApplicationMapper categoryApplicationMapper;

    @Override
    public CategoryResult execute(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));

        return categoryApplicationMapper.toResult(category);
    }
}
