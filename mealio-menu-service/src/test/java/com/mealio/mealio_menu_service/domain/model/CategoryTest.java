package com.mealio.mealio_menu_service.domain.model;

import org.junit.jupiter.api.Test;

import com.mealio.mealio_menu_service.domain.exception.CategoryNameCannotBeBlankException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CategoryTest {

    @Test
    public void shouldCreateCategory() {

        Category category = Category.create("Breakfast", "Morning meals", true);

        assertThat(category.getName())
                .isEqualTo("Breakfast");
    }

    @Test
    public void shouldThrowExceptionWhenNameIsBlank() {

        assertThatThrownBy(() ->
                Category.create("", "Description", true ))
                .isInstanceOf(CategoryNameCannotBeBlankException.class);
    }

    @Test
    public void shouldThrowExceptionWhenDescriptionIsBlank() {

        assertThatThrownBy(() ->
                Category.create("Breakfast", "   ", true))
                .isInstanceOf(CategoryNameCannotBeBlankException.class);
    }
}
