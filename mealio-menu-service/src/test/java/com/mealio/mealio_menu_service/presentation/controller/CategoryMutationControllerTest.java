package com.mealio.mealio_menu_service.presentation.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealio.mealio_menu_service.application.command.UpdateCategoryCommand;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.category.BulkCreateCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.CreateCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.DeleteCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.GetCategoryByIdUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.ListCategoriesUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.UpdateCategoryUseCase;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.exception.CategoryNotFoundException;
import com.mealio.mealio_menu_service.presentation.advice.GlobalExceptionHandler;

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryMutationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private CreateCategoryUseCase createCategoryUseCase;
    @MockitoBean
    private BulkCreateCategoryUseCase bulkCreateCategoryUseCase;
    @MockitoBean
    private ListCategoriesUseCase listCategoriesUseCase;
    @MockitoBean
    private GetCategoryByIdUseCase getCategoryByIdUseCase;
    @MockitoBean
    private UpdateCategoryUseCase updateCategoryUseCase;
    @MockitoBean
    private DeleteCategoryUseCase deleteCategoryUseCase;

    @Test
    void shouldUpdateCategoryAndReturnResult() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        UpdateCategoryCommand command = new UpdateCategoryCommand("Lunch", "Afternoon meals");
        when(updateCategoryUseCase.execute(id, command))
                .thenReturn(new CategoryResult(id, "Lunch", "Afternoon meals", false));

        // When / Then
        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Lunch"))
                .andExpect(jsonPath("$.description").value("Afternoon meals"))
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.createdBy").doesNotExist());
        verify(updateCategoryUseCase).execute(id, command);
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "description"})
    void shouldRejectBlankFieldsBeforeUpdating(String field) throws Exception {
        // Given
        Map<String, String> body = new java.util.HashMap<>(Map.of("name", "Lunch", "description", "Meals"));
        body.put(field, " ");

        // When / Then
        mockMvc.perform(put("/api/v1/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateCategoryUseCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "description"})
    void shouldRejectOversizedFieldsBeforeUpdating(String field) throws Exception {
        // Given
        Map<String, String> body = new java.util.HashMap<>(Map.of("name", "Lunch", "description", "Meals"));
        body.put(field, "x".repeat(field.equals("name") ? 101 : 501));

        // When / Then
        mockMvc.perform(put("/api/v1/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateCategoryUseCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"name\":\"Lunch\"}"})
    void shouldRejectInvalidUpdateBody(String body) throws Exception {
        // When / Then
        mockMvc.perform(put("/api/v1/categories/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateCategoryUseCase);
    }

    @Test
    void shouldReturnNotFoundOnUpdate() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        UpdateCategoryCommand command = new UpdateCategoryCommand("Lunch", "Meals");
        when(updateCategoryUseCase.execute(id, command))
                .thenThrow(new CategoryNotFoundException(id.toString()));

        // When / Then
        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void shouldReturnConflictOnDuplicateUpdate() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        UpdateCategoryCommand command = new UpdateCategoryCommand("Lunch", "Meals");
        when(updateCategoryUseCase.execute(id, command))
                .thenThrow(new CategoryAlreadyExistsException("Lunch"));

        // When / Then
        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectInvalidUuidOnUpdate() throws Exception {
        // When / Then
        mockMvc.perform(put("/api/v1/categories/not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lunch\",\"description\":\"Meals\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateCategoryUseCase);
    }

    @Test
    void shouldDeleteCategoryAndReturnNoContent() throws Exception {
        // Given
        UUID id = UUID.randomUUID();

        // When / Then
        mockMvc.perform(delete("/api/v1/categories/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(deleteCategoryUseCase).execute(id);
    }

    @Test
    void shouldReturnNotFoundOnDelete() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        doThrow(new CategoryNotFoundException(id.toString())).when(deleteCategoryUseCase).execute(id);

        // When / Then
        mockMvc.perform(delete("/api/v1/categories/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void shouldRejectInvalidUuidOnDelete() throws Exception {
        // When / Then
        mockMvc.perform(delete("/api/v1/categories/not-a-uuid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(deleteCategoryUseCase);
    }
}
