package com.mealio.mealio_menu_service.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank
    @Size(max = 100)
    String name,

    @Size(max = 500)
    String description
) {
    
}
