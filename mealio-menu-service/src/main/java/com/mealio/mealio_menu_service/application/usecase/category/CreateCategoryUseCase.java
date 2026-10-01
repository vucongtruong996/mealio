package com.mealio.mealio_menu_service.application.usecase.category;

import org.springframework.lang.NonNull;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;

public interface CreateCategoryUseCase {

    public CategoryResult execute(@NonNull CreateCategoryCommand command) throws CategoryAlreadyExistsException;
}