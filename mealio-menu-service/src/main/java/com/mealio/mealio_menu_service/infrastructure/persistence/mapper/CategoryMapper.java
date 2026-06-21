package com.mealio.mealio_menu_service.infrastructure.persistence.mapper;

import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.infrastructure.persistence.entity.CategoryEntity;

public final class CategoryMapper {
    private CategoryMapper() {
    }

    public static CategoryEntity toEntity(Category category) {
        
        return new CategoryEntity(
                category.getId()
                , category.getName()
                , category.getDescription());
    }

    public static Category toDomain(CategoryEntity categoryEntity) {
        return Category.restore(
                categoryEntity.getId()
                , categoryEntity.getName()
                , categoryEntity.getDescription());
    }
}
