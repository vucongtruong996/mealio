package com.mealio.mealio_menu_service.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.mealio.mealio_menu_service.domain.model.Category;

public interface CategoryRepository {
    Category save(Category category);

    Optional<Category> findById(UUID id);

    Optional<Category> findByName(String name);

}
