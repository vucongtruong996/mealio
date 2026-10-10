package com.mealio.mealio_menu_service.application.usecase;

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

import com.mealio.mealio_menu_service.application.usecase.implement.DeleteCategoryUseCaseImpl;
import com.mealio.mealio_menu_service.domain.exception.CategoryNotFoundException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class DeleteCategoryUseCaseTest {
    @Mock
    private CategoryRepository repository;
    @InjectMocks
    private DeleteCategoryUseCaseImpl useCase;

    @Test
    void shouldDeleteExistingCategory() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(
                Category.restore(id, "Breakfast", "Morning meals", true)));

        // When
        useCase.execute(id);

        // Then
        verify(repository).deleteById(id);
    }

    @Test
    void shouldRejectMissingCategoryWithoutDeleting() {
        // Given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.execute(id)).isInstanceOf(CategoryNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void shouldRejectNullIdBeforeRepositoryAccess() {
        // When / Then
        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repository);
    }
}
