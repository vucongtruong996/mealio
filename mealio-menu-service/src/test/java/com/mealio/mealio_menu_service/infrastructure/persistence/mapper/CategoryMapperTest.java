package com.mealio.mealio_menu_service.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.infrastructure.persistence.entity.CategoryEntity;

public class CategoryMapperTest {
    @Test
    public void shouldMapDomainToEntity() {
        //Arrange
        Category category = Category.restore(UUID.randomUUID(), "Breakfast", "Morning meals");

        //Act
        CategoryEntity categoryEntity = CategoryMapper.toEntity(category);

        //Assert
        assertEquals(category.getId(), categoryEntity.getId());
        assertEquals(category.getName(), categoryEntity.getName());
        assertEquals(category.getDescription(), categoryEntity.getDescription());
    }

    @Test
    public void shouldMapEntityToDomain() {
        //Arrange
        CategoryEntity categoryEntity = new CategoryEntity(
                UUID.randomUUID(),
                "Breakfast",
                "Morning meals"
        );

        //Act
        Category category = CategoryMapper.toDomain(categoryEntity);

        //Assert
        assertEquals(categoryEntity.getId(), category.getId());
        assertEquals(categoryEntity.getName(), category.getName());
        assertEquals(categoryEntity.getDescription(), category.getDescription());
    }

    @Test
    public void shouldMapDomainToEntityAndBack() {
        //Arrange
        Category category = Category.restore(UUID.randomUUID(), "Breakfast", "Morning meals");

        Category restoredCategory = CategoryMapper.toDomain(CategoryMapper.toEntity(category));

        //Assert
        assertEquals(category.getId(), restoredCategory.getId());
        assertEquals(category.getName(), restoredCategory.getName());   
        assertEquals(category.getDescription(), restoredCategory.getDescription());
    }
}
