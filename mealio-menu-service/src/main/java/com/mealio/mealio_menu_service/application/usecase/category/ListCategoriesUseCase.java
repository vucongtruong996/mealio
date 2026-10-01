package com.mealio.mealio_menu_service.application.usecase.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.mealio.mealio_menu_service.application.result.CategoryResult;

public interface ListCategoriesUseCase {
    
    Page<CategoryResult> listCategories(Pageable pageable);
}