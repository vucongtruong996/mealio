package com.mealio.mealio_menu_service.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

import com.mealio.mealio_menu_service.infrastructure.audit.AuditableEntity;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryEntity extends AuditableEntity{

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active;

     public CategoryEntity(
            UUID id,
            String name,
            String description,
            boolean active
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public void changeName(String name) {
    this.name = name;
}

public void changeDescription(String description) {
    this.description = description;
}

public void activate() {
    this.active = true;
}

public void deactivate() {
    this.active = false;
}
}