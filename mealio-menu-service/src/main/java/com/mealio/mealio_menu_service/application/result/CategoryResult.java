package com.mealio.mealio_menu_service.application.result;

import java.util.UUID;

public record CategoryResult(
    UUID id,
    String name,
    String description
) {
}
