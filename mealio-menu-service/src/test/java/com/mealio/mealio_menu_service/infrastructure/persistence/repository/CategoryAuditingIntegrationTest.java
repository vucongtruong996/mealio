package com.mealio.mealio_menu_service.infrastructure.persistence.repository;

import com.mealio.mealio_menu_service.infrastructure.audit.CurrentActorProvider;
import com.mealio.mealio_menu_service.infrastructure.audit.JpaAuditConfiguration;
import com.mealio.mealio_menu_service.infrastructure.persistence.entity.CategoryEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
        JpaAuditConfiguration.class,
        CategoryAuditingIntegrationTest.TestAuditConfiguration.class
})
public class CategoryAuditingIntegrationTest {

    @TestConfiguration
    static class TestAuditConfiguration {

        @Bean
        CurrentActorProvider currentActorProvider() {
            return () -> "TEST_USER";
        }
    }

    @Autowired
    private JpaCategoryRepository repository;

    @Test
    @DisplayName("Should populate audit fields when creating category")
    public void shouldPopulateAuditFieldsOnCreate() {

        // Arrange
        CategoryEntity entity = new CategoryEntity(
                UUID.randomUUID(),
                "Breakfast",
                "Morning meal",
                true
        );

        // Act
        CategoryEntity saved = repository.saveAndFlush(entity);

        // Assert
        assertThat(saved.getCreatedBy()).isEqualTo("TEST_USER");
        assertThat(saved.getUpdatedBy()).isEqualTo("TEST_USER");

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(saved.getCreatedAt())
                .isBeforeOrEqualTo(Instant.now());

        assertThat(saved.getUpdatedAt())
                .isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("Should update audit fields when updating category")
    public void shouldPopulateAuditFieldsOnUpdate() {

        // Arrange
        CategoryEntity entity = repository.saveAndFlush(
                new CategoryEntity(
                        UUID.randomUUID(),
                        "Breakfast",
                        "Morning meal",
                        true
                )
        );

        Instant createdAt = entity.getCreatedAt();

        // Give the timestamps a chance to differ
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }

        // Act
        entity.changeDescription("Updated description");

        CategoryEntity updated = repository.saveAndFlush(entity);

        // Assert
        assertThat(updated.getCreatedBy()).isEqualTo("TEST_USER");
        assertThat(updated.getUpdatedBy()).isEqualTo("TEST_USER");

        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

}