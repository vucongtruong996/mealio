package com.mealio.mealio_menu_service.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.implement.BulkCreateCategoryUseCaseImpl;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.exception.CategoryNameCannotBeBlankException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class BulkCreateCategoryUseCaseTest {
    @Mock
    private CategoryRepository repository;
    @Mock
    private CategoryApplicationMapper mapper;
    @InjectMocks
    private BulkCreateCategoryUseCaseImpl useCase;

    @Test
    void shouldCreateNormalizedCategoriesInRequestOrder() {
        // Given
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResult(any())).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            return new CategoryResult(category.getId(), category.getName(),
                    category.getDescription(), category.isActive());
        });

        // When
        List<CategoryResult> result = useCase.executeBulk(List.of(
                new CreateCategoryCommand(" Breakfast ", " Morning meals "),
                new CreateCategoryCommand("Lunch", "Afternoon meals")));

        // Then
        assertThat(result).extracting(CategoryResult::name).containsExactly("Breakfast", "Lunch");
        assertThat(result).extracting(CategoryResult::description)
                .containsExactly("Morning meals", "Afternoon meals");
        assertThat(result).allMatch(CategoryResult::isActive);
        verify(repository).findExistingNames(Set.of("Breakfast", "Lunch"));
        verify(repository, never()).existsByName(any());
    }

    @Test
    void shouldRejectNormalizedDuplicatesWithoutSaving() {
        // When / Then
        assertThatThrownBy(() -> useCase.executeBulk(List.of(
                new CreateCategoryCommand("Breakfast", "Meals"),
                new CreateCategoryCommand(" Breakfast ", "Other meals"))))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        verify(repository, never()).saveAll(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldRejectExistingNameBeforeSavingAnyCategory() {
        // Given
        when(repository.findExistingNames(Set.of("Breakfast", "Lunch"))).thenReturn(Set.of("Lunch"));

        // When / Then
        assertThatThrownBy(() -> useCase.executeBulk(List.of(
                new CreateCategoryCommand("Breakfast", "Meals"),
                new CreateCategoryCommand(" Lunch ", "Other meals"))))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        verify(repository, never()).saveAll(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldReportFirstConflictInRequestOrder() {
        // Given
        when(repository.findExistingNames(Set.of("Breakfast", "Lunch")))
                .thenReturn(Set.of("Lunch", "Breakfast"));

        // When / Then
        assertThatThrownBy(() -> useCase.executeBulk(List.of(
                new CreateCategoryCommand("Breakfast", "Meals"),
                new CreateCategoryCommand("Lunch", "Other meals"))))
                .isInstanceOf(CategoryAlreadyExistsException.class)
                .hasMessage("Category 'Breakfast' already exists.");
        verify(repository, never()).saveAll(any());
    }

    @Test
    void shouldUseOneConflictLookupForMaximumBatch() {
        // Given
        List<CreateCategoryCommand> commands = IntStream.range(0, 100)
                .mapToObj(i -> new CreateCategoryCommand("Category " + i, "Meals")).toList();
        Set<String> names = commands.stream().map(CreateCategoryCommand::name)
                .collect(java.util.stream.Collectors.toSet());
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResult(any())).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            return new CategoryResult(category.getId(), category.getName(),
                    category.getDescription(), category.isActive());
        });

        // When / Then
        assertThat(useCase.executeBulk(commands)).hasSize(100);
        verify(repository).findExistingNames(names);
        verify(repository, never()).existsByName(any());
    }

    @Test
    void shouldValidateEntireBatchBeforeAccessingRepository() {
        // When / Then
        assertThatThrownBy(() -> useCase.executeBulk(List.of(
                new CreateCategoryCommand("Breakfast", "Meals"),
                new CreateCategoryCommand(" ", "Other meals"))))
                .isInstanceOf(CategoryNameCannotBeBlankException.class);
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void shouldRejectInvalidBatchShapesBeforeAccessingRepository() {
        // When / Then
        assertThatThrownBy(() -> useCase.executeBulk(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.executeBulk(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.executeBulk(Arrays.asList((CreateCategoryCommand) null)))
                .isInstanceOf(IllegalArgumentException.class);
        List<CreateCategoryCommand> oversized = IntStream.range(0, 101)
                .mapToObj(i -> new CreateCategoryCommand("Category " + i, "Meals")).toList();
        assertThatThrownBy(() -> useCase.executeBulk(oversized)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void shouldAllowNamesDifferingOnlyByCase() {
        // Given
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResult(any())).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            return new CategoryResult(category.getId(), category.getName(),
                    category.getDescription(), category.isActive());
        });

        // When / Then
        assertThat(useCase.executeBulk(List.of(new CreateCategoryCommand("Breakfast", "Meals"),
                new CreateCategoryCommand("breakfast", "Other meals"))))
                .extracting(CategoryResult::name).containsExactly("Breakfast", "breakfast");
    }
}
