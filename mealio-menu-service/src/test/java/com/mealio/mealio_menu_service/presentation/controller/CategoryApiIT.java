package com.mealio.mealio_menu_service.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.mealio.mealio_menu_service.infrastructure.persistence.entity.CategoryEntity;
import com.mealio.mealio_menu_service.infrastructure.persistence.repository.AbstractPostgresIntegrationTest;
import com.mealio.mealio_menu_service.infrastructure.persistence.repository.JpaCategoryRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryApiIT extends AbstractPostgresIntegrationTest {
    private static final String BASE = "/api/v1/categories";

    @Autowired
    private TestRestTemplate client;
    @Autowired
    private JpaCategoryRepository repository;

    @BeforeEach
    void clearCategories() {
        repository.deleteAllInBatch();
    }

    @Test
    void shouldCompleteCategoryLifecycleOverHttp() {
        // Given / When: no mocked use cases, test audit beans, or outer transaction.
        ResponseEntity<JsonNode> created = request(HttpMethod.POST, BASE,
                "{\"name\":\" Breakfast \",\"description\":\" Morning meals \"}");

        // Then
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String id = created.getBody().get("id").asText();
        assertThat(created.getHeaders().getLocation().toString()).isEqualTo(BASE + "/" + id);
        assertThat(created.getBody().get("name").asText()).isEqualTo("Breakfast");
        assertThat(created.getBody().get("description").asText()).isEqualTo("Morning meals");
        assertThat(created.getBody().get("isActive").asBoolean()).isTrue();
        assertThat(created.getBody().size()).isEqualTo(4);
        assertThat(request(HttpMethod.GET, BASE + "/" + id, null).getBody()).isEqualTo(created.getBody());

        ResponseEntity<JsonNode> updated = request(HttpMethod.PUT, BASE + "/" + id,
                "{\"name\":\"Lunch\",\"description\":\"Afternoon meals\"}");
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("id").asText()).isEqualTo(id);
        assertThat(updated.getBody().get("name").asText()).isEqualTo("Lunch");
        assertThat(updated.getBody().get("isActive").asBoolean()).isTrue();
        assertThat(repository.findById(UUID.fromString(id))).get()
                .extracting(CategoryEntity::getDescription).isEqualTo("Afternoon meals");

        ResponseEntity<JsonNode> deleted = request(HttpMethod.DELETE, BASE + "/" + id, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(deleted.getBody()).isNull();
        assertThat(repository.findById(UUID.fromString(id))).isEmpty();
        assertThat(request(HttpMethod.GET, BASE + "/" + id, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(HttpMethod.DELETE, BASE + "/" + id, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectConflictsWithoutPartialWritesOverHttp() {
        // Given
        assertThat(request(HttpMethod.POST, BASE,
                "{\"name\":\"Breakfast\",\"description\":\"Meals\"}").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // When / Then
        assertThat(request(HttpMethod.POST, BASE,
                "{\"name\":\" Breakfast \",\"description\":\"Other meals\"}").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(request(HttpMethod.POST, BASE + "/bulk", """
                [{"name":"Lunch","description":"Meals"},
                 {"name":"Breakfast","description":"Other meals"}]
                """).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByName("Lunch")).isEmpty();
        assertThat(request(HttpMethod.POST, BASE + "/bulk", """
                [{"name":"Dinner","description":"Meals"},
                 {"name":" Dinner ","description":"Other meals"}]
                """).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repository.count()).isEqualTo(1);

        ResponseEntity<JsonNode> lunch = request(HttpMethod.POST, BASE,
                "{\"name\":\"Lunch\",\"description\":\"Meals\"}");
        String lunchId = lunch.getBody().get("id").asText();
        assertThat(request(HttpMethod.PUT, BASE + "/" + lunchId,
                "{\"name\":\" Breakfast \",\"description\":\"Changed\"}").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(repository.findById(UUID.fromString(lunchId))).get()
                .extracting(CategoryEntity::getName).isEqualTo("Lunch");
    }

    @Test
    void shouldCreateBulkInOrderAndSupportListingQueryParameters() {
        // When
        ResponseEntity<JsonNode> batch = request(HttpMethod.POST, BASE + "/bulk", """
                [{"name":"Zebra","description":"Meals"},
                 {"name":"Apple","description":"Other meals"}]
                """);

        // Then
        assertThat(batch.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(batch.getBody().get(0).get("name").asText()).isEqualTo("Zebra");
        assertThat(batch.getBody().get(1).get("name").asText()).isEqualTo("Apple");
        assertThat(repository.count()).isEqualTo(2);

        ResponseEntity<JsonNode> page = request(HttpMethod.GET, BASE + "?page=0&size=1&sort=name,asc", null);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody().isArray()).isTrue();
        assertThat(page.getBody().size()).isEqualTo(1);
        assertThat(page.getBody().get(0).get("name").asText()).isEqualTo("Apple");
        assertThat(request(HttpMethod.GET, BASE + "?page=2&size=1", null).getBody()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]", "[{}]", "[", "null",
            "[{\"name\":\"Breakfast\"}]",
            "[{\"name\":\" \",\"description\":\"Meals\"}]"})
    void shouldRejectInvalidBulkOverHttpWithoutWrites(String body) {
        // When / Then
        assertThat(request(HttpMethod.POST, BASE + "/bulk", body).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldRejectInvalidIdentifiersAndMissingResourcesOverHttp() {
        // Given
        String missing = BASE + "/" + UUID.randomUUID();
        String validBody = "{\"name\":\"Lunch\",\"description\":\"Meals\"}";

        // When / Then
        ResponseEntity<JsonNode> notFound = request(HttpMethod.GET, missing, null);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody().get("code").asText()).isEqualTo("CATEGORY_NOT_FOUND");
        assertThat(request(HttpMethod.PUT, missing, validBody).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(HttpMethod.DELETE, missing, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(HttpMethod.GET, BASE + "/not-a-uuid", null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(HttpMethod.PUT, BASE + "/not-a-uuid", validBody).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(HttpMethod.DELETE, BASE + "/not-a-uuid", null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldPopulateRealApplicationAuditAndPreserveCreationFieldsOnUpdate() {
        // Given
        ResponseEntity<JsonNode> created = request(HttpMethod.POST, BASE,
                "{\"name\":\"Breakfast\",\"description\":\"Meals\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID id = UUID.fromString(created.getBody().get("id").asText());
        CategoryEntity before = repository.findById(id).orElseThrow();
        assertThat(before.getCreatedBy()).isEqualTo("SYSTEM");
        assertThat(before.getCreatedAt()).isNotNull();
        Instant createdAt = before.getCreatedAt();

        // When
        assertThat(request(HttpMethod.PUT, BASE + "/" + id,
                "{\"name\":\"Lunch\",\"description\":\"Updated\"}").getStatusCode()).isEqualTo(HttpStatus.OK);

        // Then
        CategoryEntity after = repository.findById(id).orElseThrow();
        assertThat(after.getCreatedBy()).isEqualTo("SYSTEM");
        assertThat(after.getCreatedAt()).isEqualTo(createdAt);
        assertThat(after.getUpdatedBy()).isEqualTo("SYSTEM");
        assertThat(after.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    private ResponseEntity<JsonNode> request(HttpMethod method, String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return client.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
