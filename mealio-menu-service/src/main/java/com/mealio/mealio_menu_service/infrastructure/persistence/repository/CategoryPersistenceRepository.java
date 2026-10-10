package com.mealio.mealio_menu_service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mealio.mealio_menu_service.domain.model.Category;
import com.mealio.mealio_menu_service.domain.repository.CategoryRepository;
import com.mealio.mealio_menu_service.infrastructure.persistence.entity.CategoryEntity;
import com.mealio.mealio_menu_service.infrastructure.persistence.mapper.CategoryPersistenceMapper;

import java.util.Objects;
import java.util.Collection;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Repository
@RequiredArgsConstructor
public class CategoryPersistenceRepository implements CategoryRepository {

    private final JpaCategoryRepository jpaCategoryRepository;
    private final CategoryPersistenceMapper categoryPersistenceMapper;

    @Override
    @Transactional
    public @NonNull Category save(@NonNull Category category) {
        Objects.requireNonNull(category, "category must not be null");

        CategoryEntity entity = jpaCategoryRepository.findById(category.getId())
                .orElseGet(() -> categoryPersistenceMapper.toEntity(category));
        entity.changeName(category.getName());
        entity.changeDescription(category.getDescription());
        if (category.isActive()) {
            entity.activate();
        } else {
            entity.deactivate();
        }
        CategoryEntity savedEntity = Objects.requireNonNull(jpaCategoryRepository.save(entity), "saved entity must not be null");

        return categoryPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Category> findById(@NonNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        Optional<CategoryEntity> entity = jpaCategoryRepository.findById(id);
        if (entity.isPresent()) {
            CategoryEntity categoryEntity = Objects.requireNonNull(entity.get(), "categoryEntity must not be null");
            return Optional.of(categoryPersistenceMapper.toDomain(categoryEntity));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Category> findByName(@NonNull String name) {
        Objects.requireNonNull(name, "name must not be null");

        Optional<CategoryEntity> entity = jpaCategoryRepository.findByName(name);
        if (entity.isPresent()) {
            CategoryEntity categoryEntity = Objects.requireNonNull(entity.get(), "categoryEntity must not be null");
            return Optional.of(categoryPersistenceMapper.toDomain(categoryEntity));
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByName(@NonNull String name) {
        Objects.requireNonNull(name, "name must not be null");

        return jpaCategoryRepository.existsByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findExistingNames(Collection<String> names) {
        Objects.requireNonNull(names, "names must not be null");
        if (names.isEmpty()) {
            return Set.of();
        }
        return jpaCategoryRepository.findExistingNames(names);
    }

    @Override
    public @NonNull Page<Category> findAll(@NonNull Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");

        return jpaCategoryRepository.findAll(pageable)
                .map(categoryPersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(@NonNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        jpaCategoryRepository.deleteById(id);
    }

    @Override
    public Iterable<Category> saveAll(Iterable<Category> categories) {
        Objects.requireNonNull(categories, "categories must not be null");

        Iterable<CategoryEntity> entities = StreamSupport.stream(categories.spliterator(), false)
                .map(categoryPersistenceMapper::toEntity)
                .toList();
                
        return jpaCategoryRepository.saveAll(entities)
                .stream()
                .map(categoryPersistenceMapper::toDomain)
                .toList();
    }

}
