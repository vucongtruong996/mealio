package com.mealio.mealio_menu_service.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mealio.mealio_menu_service.application.command.UpdateCategoryCommand;
import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.implement.UpdateCategoryUseCaseImpl;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.exception.CategoryNameCannotBeBlankException;
import com.mealio.mealio_menu_service.domain.exception.CategoryNotFoundException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class UpdateCategoryUseCaseTest {
    @Mock
    private CategoryRepository repository;
    @Mock
    private CategoryApplicationMapper mapper;
    @InjectMocks
    private UpdateCategoryUseCaseImpl useCase;

    @Test
    void shouldUpdateNormalizedFieldsAndPreserveIdentityAndActiveState() {
        // Given
        UUID id = UUID.randomUUID();
        Category existing = Category.restore(id, "Breakfast", "Morning meals", false);
        CategoryResult expected = new CategoryResult(id, "Lunch", "Afternoon meals", false);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.findByName("Lunch")).thenReturn(Optional.empty());
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResult(existing)).thenReturn(expected);

        // When
        CategoryResult result = useCase.execute(id,
                new UpdateCategoryCommand(" Lunch ", " Afternoon meals "));

        // Then
        assertThat(result).isEqualTo(expected);
        assertThat(existing.getName()).isEqualTo("Lunch");
        assertThat(existing.getDescription()).isEqualTo("Afternoon meals");
        assertThat(existing.getId()).isEqualTo(id);
        assertThat(existing.isActive()).isFalse();
        verify(repository).save(existing);
    }

    @Test
    void shouldAllowUnchangedNameOwnedBySameCategory() {
        // Given
        UUID id = UUID.randomUUID();
        Category existing = Category.restore(id, "Breakfast", "Morning meals", true);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.findByName("Breakfast")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        CategoryResult expected = new CategoryResult(id, "Breakfast", "Updated meals", true);
        when(mapper.toResult(existing)).thenReturn(expected);

        // When / Then
        assertThat(useCase.execute(id, new UpdateCategoryCommand("Breakfast", "Updated meals")))
                .isEqualTo(expected);
    }

    @Test
    void shouldRejectNameOwnedByAnotherCategory() {
        // Given
        UUID id = UUID.randomUUID();
        Category existing = Category.restore(id, "Breakfast", "Morning meals", true);
        Category other = Category.create("Lunch", "Afternoon meals", true);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.findByName("Lunch")).thenReturn(Optional.of(other));

        // When / Then
        assertThatThrownBy(() -> useCase.execute(id, new UpdateCategoryCommand(" Lunch ", "Updated")))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldRejectMissingCategoryWithoutSaving() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.execute(id, new UpdateCategoryCommand("Lunch", "Meals")))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldRejectInvalidDomainInputWithoutSaving() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(
                Category.restore(id, "Breakfast", "Morning meals", true)));

        // When / Then
        assertThatThrownBy(() -> useCase.execute(id, new UpdateCategoryCommand(" ", "Meals")))
                .isInstanceOf(CategoryNameCannotBeBlankException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldRejectNullArgumentsBeforeRepositoryAccess() {
        // When / Then
        assertThatThrownBy(() -> useCase.execute(null, new UpdateCategoryCommand("Lunch", "Meals")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repository, mapper);
    }
}
