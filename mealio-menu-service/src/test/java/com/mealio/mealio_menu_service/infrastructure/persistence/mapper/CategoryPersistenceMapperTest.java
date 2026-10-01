package com.mealio.mealio_menu_service.infrastructure.persistence.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.infrastructure.persistence.entity.CategoryEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CategoryPersistenceMapperTest {

    private CategoryPersistenceMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new CategoryPersistenceMapper();
    }

    @Test
    @DisplayName("Should map Category to CategoryEntity")
    public void shouldMapDomainToEntity() {

        // Given
        UUID id = UUID.randomUUID();
        Category category = Category.restore(
                id,
                "Breakfast",
                "Morning meals",
                true
        );

        // When
        CategoryEntity entity = mapper.toEntity(category);

        // Then
        assertThat(entity).isNotNull();

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Breakfast");
        assertThat(entity.getDescription()).isEqualTo("Morning meals");
        assertThat(entity.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should map CategoryEntity to Category")
    public void shouldMapEntityToDomain() {

        // Given
        UUID id = UUID.randomUUID();

        CategoryEntity entity = new CategoryEntity(
                id,
                "Lunch",
                "Lunch menu",
                true
        );

        // When
        Category category = mapper.toDomain(entity);

        // Then
        assertThat(category).isNotNull();

        assertThat(category.getId()).isEqualTo(id);
        assertThat(category.getName()).isEqualTo("Lunch");
        assertThat(category.getDescription()).isEqualTo("Lunch menu");
        assertThat(category.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw when Category is null")
    public void shouldThrowWhenDomainIsNull() {
        assertThatThrownBy(() -> mapper.toEntity(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("category must not be null");
    }

    @Test
    @DisplayName("Should throw when CategoryEntity is null")
    public void shouldThrowWhenEntityIsNull() {
        assertThatThrownBy(() -> mapper.toDomain(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("entity must not be null");
    }

}