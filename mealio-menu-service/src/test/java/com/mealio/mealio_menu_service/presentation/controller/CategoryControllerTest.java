package com.mealio.mealio_menu_service.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealio.mealio_menu_service.application.command.CreateCategoryCommand;
import com.mealio.mealio_menu_service.application.result.CategoryResult;
import com.mealio.mealio_menu_service.application.usecase.category.CreateCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.BulkCreateCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.GetCategoryByIdUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.UpdateCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.DeleteCategoryUseCase;
import com.mealio.mealio_menu_service.application.usecase.category.ListCategoriesUseCase;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
import com.mealio.mealio_menu_service.domain.exception.CategoryNotFoundException;
import com.mealio.mealio_menu_service.presentation.advice.GlobalExceptionHandler;
import com.mealio.mealio_menu_service.presentation.request.CreateCategoryRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
public class CategoryControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private CreateCategoryUseCase createCategoryUseCase;

    @MockitoBean
    private ListCategoriesUseCase listCategoriesUseCase;

    @MockitoBean
    private BulkCreateCategoryUseCase bulkCreateCategoryUseCase;

    @MockitoBean
    private GetCategoryByIdUseCase getCategoryByIdUseCase;

    @MockitoBean
    private UpdateCategoryUseCase updateCategoryUseCase;

    @MockitoBean
    private DeleteCategoryUseCase deleteCategoryUseCase;

    @Test
    void shouldCreateBulkCategoriesInResponseOrder() throws Exception {
        // Given
        List<CreateCategoryCommand> commands = List.of(
                new CreateCategoryCommand("Breakfast", "Morning meals"),
                new CreateCategoryCommand("Lunch", "Afternoon meals"));
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        when(bulkCreateCategoryUseCase.executeBulk(commands)).thenReturn(List.of(
                new CategoryResult(firstId, "Breakfast", "Morning meals", true),
                new CategoryResult(secondId, "Lunch", "Afternoon meals", true)));

        // When / Then
        mockMvc.perform(post("/api/v1/categories/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commands)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(firstId.toString()))
                .andExpect(jsonPath("$[1].id").value(secondId.toString()))
                .andExpect(jsonPath("$[0].isActive").value(true));
        verify(bulkCreateCategoryUseCase).executeBulk(commands);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]", "[{}]", "[",
            "[{\"name\":\" \",\"description\":\"Meals\"}]",
            "[{\"name\":\"Breakfast\"}]",
            "[{\"name\":\"Breakfast\",\"description\":\" \"}]"})
    void shouldRejectInvalidBulkRequestsBeforeCallingUseCase(String json) throws Exception {
        // When / Then
        mockMvc.perform(post("/api/v1/categories/bulk")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bulkCreateCategoryUseCase);
    }

    @Test
    void shouldRejectOversizedBulkBeforeCallingUseCase() throws Exception {
        // Given
        List<CreateCategoryCommand> commands = java.util.stream.IntStream.range(0, 101)
                .mapToObj(i -> new CreateCategoryCommand("Category " + i, "Meals")).toList();

        // When / Then
        mockMvc.perform(post("/api/v1/categories/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commands)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bulkCreateCategoryUseCase);
    }

    @Test
    void shouldReturnConflictWhenBulkCategoryAlreadyExists() throws Exception {
        // Given
        List<CreateCategoryCommand> commands = List.of(new CreateCategoryCommand("Breakfast", "Meals"));
        when(bulkCreateCategoryUseCase.executeBulk(commands))
                .thenThrow(new CategoryAlreadyExistsException("Breakfast"));

        // When / Then
        mockMvc.perform(post("/api/v1/categories/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commands)))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"name\":\"Breakfast\"}",
            "{\"name\":\"Breakfast\",\"description\":\" \"}"})
    void shouldRejectInvalidDescriptionOnSingleCreate(String json) throws Exception {
        // When / Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(createCategoryUseCase);
    }

    @Test
    void shouldReturnCategoryById() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        when(getCategoryByIdUseCase.execute(id))
                .thenReturn(new CategoryResult(id, "Breakfast", "Morning meals", true));

        // When / Then
        mockMvc.perform(get("/api/v1/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Breakfast"))
                .andExpect(jsonPath("$.description").value("Morning meals"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.createdBy").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedBy").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        verify(getCategoryByIdUseCase).execute(id);
    }

    @Test
    void shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        when(getCategoryByIdUseCase.execute(id))
                .thenThrow(new CategoryNotFoundException(id.toString()));

        // When / Then
        mockMvc.perform(get("/api/v1/categories/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Category Not Found"))
                .andExpect(jsonPath("$.detail").value("Category '" + id + "' was not found."))
                .andExpect(jsonPath("$.type").value("errors/category-not-found"))
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        verify(getCategoryByIdUseCase).execute(id);
    }

    @Test
    void shouldRejectInvalidCategoryIdBeforeCallingUseCase() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/v1/categories/not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getCategoryByIdUseCase);
    }

    @Test
    @DisplayName("should create category successfully")
    public void shouldCreateCategorySuccessfully() throws JsonProcessingException, Exception {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Breakfast"
                , "Morning meals");
        
        CategoryResult response = new CategoryResult(
                categoryId
                , "Breakfast"
                , "Morning meals", false); 
        
        when(createCategoryUseCase.execute(any(CreateCategoryCommand.class)))
                .thenReturn(response);
        
        // Act & assert
        mockMvc.perform(
            post("/api/v1/categories")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request))
        )
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/categories/" + categoryId.toString()))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(categoryId.toString()))
        .andExpect(jsonPath("$.name").value("Breakfast"))
        .andExpect(jsonPath("$.description").value("Morning meals"));

        verify(createCategoryUseCase).execute(ArgumentMatchers.any(CreateCategoryCommand.class));
    }
    
    @Test
    @DisplayName("should return 400 when name is blank")
    public void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        // Arrange

        CreateCategoryRequest request = new CreateCategoryRequest(
                ""
                , "Morning meals");
        
        mockMvc.perform(
            post("/api/v1/categories")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    public void shouldReturnBadRequestWhenRequestBodyIsInvalid() throws Exception {
        // Arrange
            String json = """
                {
                    "description":"Morning meals"
                }
                """;
        
        mockMvc.perform(
            post("/api/v1/categories")
            .contentType("application/json")
            .content(json)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 409 when category already exists") 
    public void shouldReturnConflictWhenCategoryAlreadyExists() throws Exception {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Breakfast"
                , "Morning meals");
        
        when(createCategoryUseCase.execute(any(CreateCategoryCommand.class)))
                .thenThrow(new CategoryAlreadyExistsException(request.name()));
        
        // Act & assert
        mockMvc.perform(
            post("/api/v1/categories")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Category Already Exists"))
        .andExpect(jsonPath("$.detail").value("Category Breakfast already exists."))
        .andExpect(jsonPath("$.type").value("errors/category-already-exists"));
    }

    @Test
    @DisplayName("Should return 415 when unsupported media type is used")
    public void shouldReturnUnsupportedMediaType() throws Exception {
        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Breakfast"
                , "Morning meals");
        
        // Act & assert
        mockMvc.perform(
            post("/api/v1/categories")
            .contentType("text/plain")
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Should return categories successfully")
    public void shouldReturnCategoriesSuccessfully() throws Exception {
        // Arrange
        UUID categoryId1 = UUID.randomUUID();
        UUID categoryId2 = UUID.randomUUID();
        
        CategoryResult category1 = new CategoryResult(categoryId1, "Breakfast", "Morning meals", false);
        CategoryResult category2 = new CategoryResult(categoryId2, "Lunch", "Afternoon meals", false);
        
        List<CategoryResult> categories = List.of(category1, category2);
        
        when(listCategoriesUseCase.listCategories(any(Pageable.class)))
            .thenReturn(new PageImpl<>(categories));
        
        // Act & assert
        mockMvc.perform(get("/api/v1/categories"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(categoryId1.toString()))
            .andExpect(jsonPath("$[0].name").value("Breakfast"))
            .andExpect(jsonPath("$[0].description").value("Morning meals"))
            .andExpect(jsonPath("$[1].id").value(categoryId2.toString()))
            .andExpect(jsonPath("$[1].name").value("Lunch"))
            .andExpect(jsonPath("$[1].description").value("Afternoon meals"));
        
        verify(listCategoriesUseCase).listCategories(any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty page when no categories exist")
    public void shouldReturnEmptyPageWhenNoCategoriesExist() throws Exception {
        // Arrange
        when(listCategoriesUseCase.listCategories(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        
        // Act & assert
        mockMvc.perform(get("/api/v1/categories"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(0));
        
        verify(listCategoriesUseCase).listCategories(any(Pageable.class));
    }



    @Test
    @DisplayName("Should support pagination")
    public void shouldSupportPagination(){
        
    }

    @Test
    @DisplayName("Should support sorting by name")
    public void shouldSupportSortingByName() {

    }

}
