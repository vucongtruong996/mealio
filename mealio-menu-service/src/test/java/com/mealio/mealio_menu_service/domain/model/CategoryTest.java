package com.mealio.mealio_menu_service.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void shouldCreateCategory() {

        Category category = Category.create("Breakfast", "Morning meals");

        assertThat(category.getName())
                .isEqualTo("Breakfast");
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {

        assertThatThrownBy(() ->
                Category.create("", "Description"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
