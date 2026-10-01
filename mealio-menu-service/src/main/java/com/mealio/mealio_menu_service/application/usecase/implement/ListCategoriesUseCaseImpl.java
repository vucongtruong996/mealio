package com.mealio.mealio_menu_service.application.usecase.implement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.category.ListCategoriesUseCase;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListCategoriesUseCaseImpl implements ListCategoriesUseCase {
    private final CategoryRepository categoryRepository;
    private final CategoryApplicationMapper categoryApplicationMapper;

    @Override
    public Page<CategoryResult> listCategories(Pageable pageable) {
        
        return categoryRepository.findAll(pageable)
                .map(categoryApplicationMapper::toResult);
    }
}
