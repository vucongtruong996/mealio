package com.mealio.mealio_menu_service.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import com.mealio.mealio_menu_service.application.usecase.category.ListCategoriesUseCase;
import com.mealio.mealio_menu_service.domain.exception.CategoryAlreadyExistsException;
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
