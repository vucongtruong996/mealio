package com.mealio.mealio_menu_service.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.category.BulkCreateCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.CreateCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.implement.CreateCategoryUseCaseImpl;
import com.mealio.mealio_menu_service.application.usecase.implement.BulkCreateCategoryUseCaseImpl;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;
import com.mealio.mealio_menu_service.infrastructure.audit.CurrentActorProvider;
import com.mealio.mealio_menu_service.infrastructure.audit.JpaAuditConfiguration;
import com.mealio.mealio_menu_service.infrastructure.persistence.mapper.CategoryPersistenceMapper;

import jakarta.persistence.EntityManagerFactory;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({CategoryPersistenceRepository.class, CategoryPersistenceMapper.class,
        CategoryApplicationMapper.class, BulkCreateCategoryUseCaseImpl.class, CreateCategoryUseCaseImpl.class,
        JpaAuditConfiguration.class, CategoryBulkIT.AuditConfiguration.class})
class CategoryBulkIT extends AbstractPostgresIntegrationTest {
    @TestConfiguration
    static class AuditConfiguration {
        @Bean
        CurrentActorProvider currentActorProvider() {
            return () -> "BULK_TEST";
        }
    }

    @Autowired
    private BulkCreateCategoryUseCase useCase;
    @Autowired
    private CreateCategoryUseCase createUseCase;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private JpaCategoryRepository jpaRepository;
    @MockitoSpyBean
    private CategoryRepository repository;

    @BeforeEach
    void clearCategories() {
        jpaRepository.deleteAllInBatch();
    }

    @Test
    void shouldCommitEntireBatchInOrder() {
        // When
        List<CategoryResult> result = useCase.executeBulk(List.of(
                new CreateCategoryCommand(" Breakfast ", "Morning meals"),
                new CreateCategoryCommand("Lunch", "Afternoon meals")));

        // Then: these reads occur after the application transaction commits.
        assertThat(result).extracting(CategoryResult::name).containsExactly("Breakfast", "Lunch");
        assertThat(jpaRepository.count()).isEqualTo(2);
        assertThat(repository.findById(result.get(0).id())).isPresent();
        assertThat(repository.findById(result.get(1).id())).isPresent();
    }

    @Test
    void shouldRejectRequestDuplicatesWithoutAnyInsert() {
        // When / Then
        assertThatThrownBy(() -> useCase.executeBulk(List.of(
                new CreateCategoryCommand("Breakfast", "Meals"),
                new CreateCategoryCommand(" Breakfast ", "Other meals"))))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        assertThat(jpaRepository.count()).isZero();
    }

    @Test
    void shouldRejectExistingNameWithoutPartialInsert() {
        // Given
        repository.save(Category.create("Lunch", "Existing", true));

        // When / Then
        assertThatThrownBy(() -> useCase.executeBulk(List.of(
                new CreateCategoryCommand("Breakfast", "Meals"),
                new CreateCategoryCommand("Lunch", "Other meals"))))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        assertThat(jpaRepository.count()).isEqualTo(1);
        assertThat(repository.findByName("Breakfast")).isEmpty();
    }

    @Test
    void shouldRollbackEntireBatchWhenDatabaseRejectsDuplicateAfterPrecheck() {
        // Given: simulate a stale precheck; the database still sees the existing name.
        repository.save(Category.create("Lunch", "Existing", true));
        doReturn(Set.of()).when(repository).findExistingNames(Set.of("Breakfast", "Lunch"));

        // When / Then: actual save and commit are not mocked.
        assertThatThrownBy(() -> useCase.executeBulk(List.of(
                new CreateCategoryCommand("Breakfast", "Meals"),
                new CreateCategoryCommand("Lunch", "Other meals"))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jpaRepository.count()).isEqualTo(1);
        assertThat(repository.findByName("Breakfast")).isEmpty();
        assertThat(repository.findByName("Lunch")).get()
                .extracting(Category::getDescription).isEqualTo("Existing");
    }

    @Test
    void shouldFetchOnlyMatchingNamesWithOneSqlQueryAndNoEntityHydration() {
        // Given
        repository.save(Category.create("Breakfast", "Morning meals", true));
        repository.save(Category.create("Lunch", "Afternoon meals", true));
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        // When
        Set<String> found = repository.findExistingNames(Set.of("Breakfast", "breakfast", "Dinner"));

        // Then
        assertThat(found).containsExactly("Breakfast");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(statistics.getEntityLoadCount()).isZero();
    }

    @Test
    void shouldReturnEmptyNamesWithoutSqlForEmptyInput() {
        // Given
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        // When / Then
        assertThat(repository.findExistingNames(Set.of())).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    @Test
    void shouldRejectSingleCreateUsingNormalizedExistingName() {
        // Given
        repository.save(Category.create("Breakfast", "Existing", true));

        // When / Then
        assertThatThrownBy(() -> createUseCase.execute(
                new CreateCategoryCommand(" Breakfast ", " New meals ")))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldCommitNormalizedSingleCreate() {
        // When
        CategoryResult created = createUseCase.execute(
                new CreateCategoryCommand(" Breakfast ", " Morning meals "));

        // Then
        assertThat(created.name()).isEqualTo("Breakfast");
        assertThat(repository.findById(created.id())).get()
                .extracting(Category::getDescription).isEqualTo("Morning meals");
    }
}
