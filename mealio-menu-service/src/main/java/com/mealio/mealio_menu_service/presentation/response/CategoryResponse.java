package com.mealio.mealio_menu_service.presentation.response;

import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String name,
    String description
) {
    
}
