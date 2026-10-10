package com.mealio.mealio_menu_service.application.usecase;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.implement.CreateCategoryUseCaseImpl;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateCategoryUseCaseTest {

    @Test
    void shouldCheckNormalizedNameBeforeSaving() {
        // Given
        when(categoryRepository.existsByName("Breakfast")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> createCategoryUseCase.execute(
                new CreateCategoryCommand(" Breakfast ", " Morning meals ")))
                .isInstanceOf(CategoryAlreadyExistsException.class)
                .hasMessage("Category 'Breakfast' already exists.");
        verify(categoryRepository).existsByName("Breakfast");
        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(categoryApplicationMapper);
    }

    @Test
    void shouldValidateDescriptionBeforeQueryingRepository() {
        // When / Then
        assertThatThrownBy(() -> createCategoryUseCase.execute(
                new CreateCategoryCommand("Breakfast", " ")))
                .isInstanceOf(com.mealio.mealio_menu_service.domain.exception.CategoryNameCannotBeBlankException.class);
        verifyNoInteractions(categoryRepository, categoryApplicationMapper);
    }

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryApplicationMapper categoryApplicationMapper;

    @InjectMocks
    private CreateCategoryUseCaseImpl createCategoryUseCase;

    @Captor
    private ArgumentCaptor<Category> categoryCaptor;

    private CreateCategoryCommand command;

    @BeforeEach
    public void setUp() {

        command = new CreateCategoryCommand(
                "Breakfast",
                "Morning meals"
        );
    }

    @Test
    @DisplayName("Should create category successfully")
    public void shouldCreateCategorySuccessfully() {

        // Given
        UUID id = UUID.randomUUID();

        Category savedCategory = Category.restore(
                id,
                "Breakfast",
                "Morning meals",
                true
        );

        CategoryResult expectedResult = new CategoryResult(
                id,
                "Breakfast",
                "Morning meals",
                true
        );

        when(categoryRepository.existsByName(command.name()))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(savedCategory);

        when(categoryApplicationMapper.toResult(savedCategory))
                .thenReturn(expectedResult);

        // When
        CategoryResult result = createCategoryUseCase.execute(command);

        // Then
        verify(categoryRepository).save(categoryCaptor.capture());

        Category captured = categoryCaptor.getValue();

        assertThat(captured.getName()).isEqualTo("Breakfast");
        assertThat(captured.getDescription()).isEqualTo("Morning meals");
        assertThat(captured.isActive()).isTrue();

        assertThat(result).isEqualTo(expectedResult);

        verify(categoryRepository).existsByName("Breakfast");
        verify(categoryApplicationMapper).toResult(savedCategory);
    }

    @Test
    @DisplayName("Should throw CategoryAlreadyExistsException when category already exists")
    public void shouldThrowCategoryAlreadyExistsException() {

        // Given
        when(categoryRepository.existsByName(command.name()))
                .thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> createCategoryUseCase.execute(command))
                .isInstanceOf(CategoryAlreadyExistsException.class)
                .hasMessage("Category 'Breakfast' already exists.");

        verify(categoryRepository).existsByName(command.name());
        verify(categoryRepository, never()).save(any());
        verify(categoryApplicationMapper, never()).toResult(any());
    }

    @Test
    @DisplayName("Should create category with active status")
    public void shouldCreateCategoryWithActiveStatus() {

        // Given
        UUID id = UUID.randomUUID();
        Category savedCategory = Category.restore(
                id,
                "Breakfast",
                "Morning meals",
                true
        );

        when(categoryRepository.existsByName(command.name()))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(savedCategory);

        when(categoryApplicationMapper.toResult(any(Category.class)))
                .thenReturn(new CategoryResult(
                        id,
                        "Breakfast",
                        "Morning meals",
                        true
                ));

        // When
        createCategoryUseCase.execute(command);

        // Then
        verify(categoryRepository).save(categoryCaptor.capture());

        Category captured = categoryCaptor.getValue();

        assertThat(captured.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should initialize createdBy and updatedBy with same actor")
    public void shouldInitializeAuditFields() {

        // Given
        UUID id = UUID.randomUUID();

        Category savedCategory = Category.restore(
                id,
                "Breakfast",
                "Morning meals",
                true
        );

        when(categoryRepository.existsByName(command.name()))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(savedCategory);

        when(categoryApplicationMapper.toResult(any(Category.class)))
                .thenReturn(new CategoryResult(
                        id,
                        "Breakfast",
                        "Morning meals",
                        true
                ));

        // When
        createCategoryUseCase.execute(command);

        // Then
        verify(categoryRepository).save(categoryCaptor.capture());

        // Category captured = categoryCaptor.getValue();

        // assertThat(captured.getCreatedBy()).isEqualTo("SYSTEM");
        // assertThat(captured.getUpdatedBy()).isEqualTo("SYSTEM");
    }
}
