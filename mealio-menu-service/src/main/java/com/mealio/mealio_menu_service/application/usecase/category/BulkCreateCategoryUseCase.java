package com.mealio.mealio_menu_service.application.usecase.category;

import java.util.List;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.result.CategoryResult;

public interface BulkCreateCategoryUseCase {
    List<CategoryResult> executeBulk(List<CreateCategoryCommand> commands);
}
