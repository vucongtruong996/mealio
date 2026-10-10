package com.mealio.mealio_menu_service.application.usecase.category;

import java.util.UUID;

import com.mealio.mealio_menu_service.application.command.UpdateCategoryCommand;
import com.mealio.mealio_menu_service.application.result.CategoryResult;

public interface UpdateCategoryUseCase {
    CategoryResult execute(UUID id, UpdateCategoryCommand command);
}
