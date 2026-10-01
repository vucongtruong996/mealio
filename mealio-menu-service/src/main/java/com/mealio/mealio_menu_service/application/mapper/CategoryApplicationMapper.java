package com.mealio.mealio_menu_service.application.mapper;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.domain.model.Category;

@Component
public class CategoryApplicationMapper {

    public @NonNull CategoryResult toResult(@NonNull Category category) {
        Objects.requireNonNull(category, "category must not be null");

        return new CategoryResult(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }

}