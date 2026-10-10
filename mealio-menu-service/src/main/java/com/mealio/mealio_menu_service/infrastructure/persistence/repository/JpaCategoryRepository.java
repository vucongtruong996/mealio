package com.mealio.mealio_menu_service.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mealio.mealio_menu_service.infrastructure.persistence.entity.CategoryEntity;

public interface JpaCategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    
    Optional<CategoryEntity> findByName(String name);   

    boolean existsByName(String name);

    @Query("select category.name from CategoryEntity category where category.name in :names")
    Set<String> findExistingNames(@Param("names") Collection<String> names);
}
