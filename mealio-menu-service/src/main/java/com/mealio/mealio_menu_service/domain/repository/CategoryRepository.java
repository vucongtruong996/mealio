package com.mealio.mealio_menu_service.domain.repository;

import java.util.Optional;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import com.mealio.mealio_menu_service.domain.model.Category;

public interface CategoryRepository {
    @NonNull
    Category save(@NonNull Category category);

    @NonNull
    Iterable<Category> saveAll(@NonNull Iterable<Category> categories);

    Optional<Category> findById(@NonNull UUID id);

    Optional<Category> findByName(@NonNull String name);

    boolean existsByName(@NonNull String name);

    Set<String> findExistingNames(Collection<String> names);

    @NonNull
    Page<Category> findAll(@NonNull Pageable pageable);

    void deleteById(@NonNull UUID id);
}
