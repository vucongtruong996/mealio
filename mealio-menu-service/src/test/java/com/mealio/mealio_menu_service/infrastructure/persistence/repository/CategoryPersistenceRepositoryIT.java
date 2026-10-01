package com.mealio.mealio_menu_service.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.infrastructure.persistence.mapper.CategoryPersistenceMapper;

@DataJpaTest
@Import({CategoryPersistenceRepository.class, CategoryPersistenceMapper.class})
public class CategoryPersistenceRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private CategoryPersistenceRepository categoryPersistenceRepository;

    @Test
    public void shouldSaveAndFindCategoryByName() {
        // Arrange
        Category category = Category.create("Breakfast", "Morning meals", true);
        categoryPersistenceRepository.save(category);

        // Act
        Optional<Category> result = categoryPersistenceRepository.findByName("Breakfast");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Breakfast");
        assertThat(result.get().getDescription()).isEqualTo("Morning meals");
        assertThat(result.get().isActive()).isTrue();
    }

    @Test
    public void shouldThrowWhenSavingNullCategory() {
        assertThatThrownBy(() -> categoryPersistenceRepository.save(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("category must not be null");
    }
}
