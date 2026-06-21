package com.mealio.mealio_menu_service.domain.model;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void shouldCreateCategory() {

        Category category = new Category(
                UUID.randomUUID(),
                "Breakfast",
                "Morning meals"
        );

        assertThat(category.getName())
                .isEqualTo("Breakfast");
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {

        assertThatThrownBy(() ->
                new Category(
                        UUID.randomUUID(),
                        "",
                        "Description"
                ))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
