package com.mealio.mealio_menu_service.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.implement.GetCategoryByIdUseCaseImpl;
import com.mealio.mealio_menu_service.domain.exception.CategoryNotFoundException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class GetCategoryByIdUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryApplicationMapper categoryApplicationMapper;

    @InjectMocks
    private GetCategoryByIdUseCaseImpl useCase;

    @Test
    void shouldReturnCategoryWhenFound() {
        // Given
        UUID id = UUID.randomUUID();
        Category category = Category.restore(id, "Breakfast", "Morning meals", true);
        CategoryResult expected = new CategoryResult(id, "Breakfast", "Morning meals", true);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryApplicationMapper.toResult(category)).thenReturn(expected);

        // When
        CategoryResult result = useCase.execute(id);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(categoryRepository).findById(id);
        verify(categoryApplicationMapper).toResult(category);
    }

    @Test
    void shouldThrowWhenCategoryDoesNotExist() {
        // Given
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category '%s' was not found.", id);
        verifyNoInteractions(categoryApplicationMapper);
    }

    @Test
    void shouldRejectNullIdBeforeAccessingRepository() {
        // When / Then
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("id must not be null");
        verifyNoInteractions(categoryRepository, categoryApplicationMapper);
    }
}
