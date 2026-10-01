package com.mealio.mealio_menu_service.application.usecase.implement;

import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.mapper.CategoryApplicationMapper;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.category.BulkCreateCategoryUseCase;
import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BulkCreateCategoryUseCaseImpl implements BulkCreateCategoryUseCase {
    private final CategoryRepository categoryRepository;
    private final CategoryApplicationMapper categoryApplicationMapper;

    @Override
    public List<CategoryResult> executeBulk(List<CreateCategoryCommand> commands) {
        List<Category> categories = commands.stream()
                .map(command -> Category.create(
                        command.name(),
                        command.description(),
                        true
                ))
                .toList();

        Iterable<Category> savedCategories =
                categoryRepository.saveAll(categories);

        return StreamSupport
                .stream(savedCategories.spliterator(), false)
                .map(categoryApplicationMapper::toResult)
                .toList();
    }
    
}
