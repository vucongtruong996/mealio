package com.mealio.mealio_menu_service.presentation.mapper;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.presentation.request.CreateCategoryRequest;
import com.mealio.mealio_menu_service.presentation.response.CategoryResponse;

public final class CategoryPresentationMapper {
    private CategoryPresentationMapper() {
        // Private constructor to prevent instantiation
    }

    public static CreateCategoryCommand toCreateCategoryCommand(CreateCategoryRequest request) {
        return new CreateCategoryCommand(
            request.name(),
            request.description()
        );
    }

    public static CategoryResponse toCategoryResponse(CategoryResult response) {
        return new CategoryResponse(
            response.id(),
            response.name(),
            response.description()
        );
    }
}