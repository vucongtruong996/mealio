package com.mealio.mealio_menu_service.application.dto;

import java.util.UUID;

public record CreateCategoryResponse(
    UUID id,
    String name,
    String description
) {
}
