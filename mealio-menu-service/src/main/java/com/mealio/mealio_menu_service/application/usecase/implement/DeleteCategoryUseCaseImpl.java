package com.mealio.mealio_menu_service.application.usecase.implement;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealio.mealio_menu_service.application.usecase.category.DeleteCategoryUseCase;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;
import com.mealio.mealio_menu_service.domain.exception.CategoryNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteCategoryUseCaseImpl implements DeleteCategoryUseCase {
    private final CategoryRepository categoryRepository;

    @Override
    public void execute(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));
        categoryRepository.deleteById(id);
    }
}
