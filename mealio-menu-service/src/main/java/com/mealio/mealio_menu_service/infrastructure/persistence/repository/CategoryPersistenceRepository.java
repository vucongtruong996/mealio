package com.mealio.mealio_menu_service.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;
import com.mealio.mealio_menu_service.infrastructure.persistence.entity.CategoryEntity;
import com.mealio.mealio_menu_service.infrastructure.persistence.mapper.CategoryMapper;

public class CategoryPersistenceRepository implements CategoryRepository {

    private final JpaCategoryRepository jpaCategoryRepository;

    public CategoryPersistenceRepository(JpaCategoryRepository jpaCategoryRepository) {
        this.jpaCategoryRepository = jpaCategoryRepository;
    }

    @Override
    public Category save(Category category) {
        
        CategoryEntity categoryEntity = CategoryMapper.toEntity(category);

        CategoryEntity savedEntity = jpaCategoryRepository.save(categoryEntity);

        return CategoryMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        
        return jpaCategoryRepository.findById(id).map(CategoryMapper::toDomain);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return jpaCategoryRepository.findByName(name).map(CategoryMapper::toDomain);
    }
    
}
