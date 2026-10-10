package com.mealio.mealio_menu_service.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.mealio.mealio_menu_service.application.command.UpdateCategoryCommand;
import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.usecase.category.DeleteCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.UpdateCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.implement.DeleteCategoryUseCaseImpl;
import com.mealio.mealio_menu_service.application.usecase.implement.UpdateCategoryUseCaseImpl;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.exception.CategoryNotFoundException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;
import com.mealio.mealio_menu_service.infrastructure.audit.CurrentActorProvider;
import com.mealio.mealio_menu_service.infrastructure.audit.JpaAuditConfiguration;
import com.mealio.mealio_menu_service.infrastructure.persistence.entity.CategoryEntity;
import com.mealio.mealio_menu_service.infrastructure.persistence.mapper.CategoryPersistenceMapper;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CategoryPersistenceRepository.class, CategoryPersistenceMapper.class,
        CategoryApplicationMapper.class, UpdateCategoryUseCaseImpl.class,
        DeleteCategoryUseCaseImpl.class, JpaAuditConfiguration.class,
        CategoryMutationIT.AuditConfiguration.class})
class CategoryMutationIT extends AbstractPostgresIntegrationTest {
    @TestConfiguration
    static class AuditConfiguration {
        @Bean
        CurrentActorProvider currentActorProvider() {
            return () -> "TEST_USER";
        }
    }

    @Autowired
    private CategoryRepository repository;
    @Autowired
    private UpdateCategoryUseCase updateUseCase;
    @Autowired
    private DeleteCategoryUseCase deleteUseCase;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistUpdateAndPreserveCreationAudit() {
        // Given
        Category saved = repository.save(Category.create("Breakfast", "Morning meals", false));
        entityManager.flush();
        entityManager.clear();
        CategoryEntity original = entityManager.find(CategoryEntity.class, saved.getId());
        Instant createdAt = original.getCreatedAt();
        assertThat(original.getCreatedBy()).isEqualTo("TEST_USER");
        assertThat(createdAt).isNotNull();

        // When
        updateUseCase.execute(saved.getId(), new UpdateCategoryCommand(" Lunch ", " Afternoon meals "));

        // Then: creation audit must survive both in memory and after reloading.
        assertThat(original.getCreatedAt()).isEqualTo(createdAt);
        assertThat(original.getCreatedBy()).isEqualTo("TEST_USER");
        entityManager.flush();
        entityManager.clear();
        CategoryEntity updated = entityManager.find(CategoryEntity.class, saved.getId());
        assertThat(updated.getName()).isEqualTo("Lunch");
        assertThat(updated.getDescription()).isEqualTo("Afternoon meals");
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getCreatedBy()).isEqualTo("TEST_USER");
        assertThat(updated.getUpdatedBy()).isEqualTo("TEST_USER");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void shouldRejectDuplicateWithoutChangingPersistedCategory() {
        // Given
        Category saved = repository.save(Category.create("Breakfast", "Morning meals", true));
        repository.save(Category.create("Lunch", "Afternoon meals", true));
        entityManager.flush();
        entityManager.clear();

        // When / Then
        assertThatThrownBy(() -> updateUseCase.execute(saved.getId(),
                new UpdateCategoryCommand("Lunch", "Changed")))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        entityManager.clear();
        CategoryEntity unchanged = entityManager.find(CategoryEntity.class, saved.getId());
        assertThat(unchanged.getName()).isEqualTo("Breakfast");
        assertThat(unchanged.getDescription()).isEqualTo("Morning meals");
    }

    @Test
    void shouldPhysicallyDeleteCategoryAndRejectRepeatedDelete() {
        // Given
        Category saved = repository.save(Category.create("Breakfast", "Morning meals", true));
        entityManager.flush();
        entityManager.clear();

        // When
        deleteUseCase.execute(saved.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(repository.findById(saved.getId())).isEmpty();
        assertThatThrownBy(() -> deleteUseCase.execute(saved.getId()))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldRejectUpdateOfMissingCategory() {
        // When / Then
        assertThatThrownBy(() -> updateUseCase.execute(UUID.randomUUID(),
                new UpdateCategoryCommand("Lunch", "Meals")))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
