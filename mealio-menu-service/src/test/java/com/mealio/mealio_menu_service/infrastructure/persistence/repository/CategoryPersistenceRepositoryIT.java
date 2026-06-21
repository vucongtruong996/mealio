package com.mealio.mealio_menu_service.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.mealio.mealio_menu_service.domain.model.Category;

@DataJpaTest
@Import(CategoryPersistenceRepository.class)
public class CategoryPersistenceRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private CategoryPersistenceRepository categoryPersistenceRepository;

    @Test
    public void shouldSaveAndFindCategoryByName() {
        // Arrange
        Category category = Category.create("Breakfast", "Morning meals");
        categoryPersistenceRepository.save(category);

        // Act
        Optional<Category> result = categoryPersistenceRepository.findByName("Breakfast");

        // Assert
        assertThat(result).isPresent();

        assertThat(result.get().getName()).isEqualTo("Breakfast");
        assertThat(result.get().getDescription()).isEqualTo("Morning meals");
    }
}
