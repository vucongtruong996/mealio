package com.mealio.mealio_menu_service.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.dto.CreateCategoryResponse;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

public class CreateCategoryUseCaseTest {
    private CategoryRepository mockCategoryRepository;

    private CreateCategoryUseCase createCategoryUseCase;

    @BeforeEach
    public void setUp() {
        mockCategoryRepository = mock(CategoryRepository.class);
        createCategoryUseCase = new CreateCategoryUseCase(mockCategoryRepository);
    }

    @Test
    public void shouldCreateCategorySuccessfully() {
        //Arrange
        CreateCategoryCommand command = new CreateCategoryCommand("Breakfast", "Morning meals");
        Category category = Category.create(command.name(), command.description());

        when(mockCategoryRepository.findByName("Breakfast")).thenReturn(Optional.empty());
        when(mockCategoryRepository.save(any(Category.class))).thenReturn(category);

        //Act
        CreateCategoryResponse response = createCategoryUseCase.execute(command);

        //Verify
        assertThat(response.name()).isEqualTo("Breakfast");
        assertThat(response.description()).isEqualTo("Morning meals");

        verify(mockCategoryRepository).findByName("Breakfast");
        verify(mockCategoryRepository).save(any(Category.class));
    }

    @Test
    public void shouldThrowExceptionWhenCategoryAlreadyExists() {
        //Arrange
        CreateCategoryCommand command = new CreateCategoryCommand("Breakfast", "Morning meals");

        when(mockCategoryRepository.findByName("Breakfast")).thenReturn(Optional.of(Category.create("Breakfast", "Morning meals")));

        //Act & Assert
        assertThatThrownBy(() -> createCategoryUseCase.execute(command))
                .isInstanceOf(CategoryAlreadyExistsException.class)
                .hasMessage("Category already exists: Breakfast");
    }
}
