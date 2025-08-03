package com.ticketly.mseventseating.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.config.SecurityConfig;
import com.ticketly.mseventseating.dto.category.CategoryRequest;
import com.ticketly.mseventseating.dto.category.CategoryResponse;
import com.ticketly.mseventseating.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private UUID categoryId;
    private CategoryRequest categoryRequest;
    private CategoryResponse categoryResponse;
    private List<CategoryResponse> categoryResponses;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();

        // Set up request data
        categoryRequest = new CategoryRequest();
        categoryRequest.setName("Test Category");
        categoryRequest.setParentId(UUID.randomUUID());

        // Set up response data
        categoryResponse = CategoryResponse.builder()
                .id(categoryId)
                .name("Test Category")
                .parentId(categoryRequest.getParentId())
                .subCategories(new HashSet<>())
                .build();

        // Set up list of responses
        CategoryResponse parentCategory = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Parent Category")
                .parentId(null)
                .subCategories(new HashSet<>(Collections.singletonList(categoryResponse)))
                .build();

        categoryResponses = List.of(parentCategory);
    }

    @Test
    @WithMockUser
    void getAllCategories_ShouldReturnAllCategories() throws Exception {
        // Given
        when(categoryService.getAllCategories()).thenReturn(categoryResponses);

        // When & Then
        mockMvc.perform(get("/v1/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Parent Category")))
                .andExpect(jsonPath("$[0].subCategories", hasSize(1)))
                .andExpect(jsonPath("$[0].subCategories[0].name", is("Test Category")));

        verify(categoryService).getAllCategories();
    }

    @Test
    @WithMockUser
    void getCategoryById_WithValidId_ShouldReturnCategory() throws Exception {
        // Given
        when(categoryService.getCategoryById(categoryId)).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(get("/v1/categories/{id}", categoryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(categoryId.toString())))
                .andExpect(jsonPath("$.name", is("Test Category")));

        verify(categoryService).getCategoryById(categoryId);
    }

    @Test
    @WithMockUser
    void createCategory_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/v1/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser(roles = "manage_categories")
    void createCategory_WithAdminRole_ShouldCreateCategory() throws Exception {
        // Given
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(post("/v1/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(categoryId.toString())))
                .andExpect(jsonPath("$.name", is("Test Category")));

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    @WithMockUser
    void updateCategory_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(put("/v1/categories/{id}", categoryId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser(roles = "manage_categories")
    void updateCategory_WithAdminRole_ShouldUpdateCategory() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class))).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(put("/v1/categories/{id}", categoryId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(categoryId.toString())))
                .andExpect(jsonPath("$.name", is("Test Category")));

        verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
    }

    @Test
    @WithMockUser
    void deleteCategory_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/v1/categories/{id}", categoryId)
                .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser(roles = "manage_categories")
    void deleteCategory_WithAdminRole_ShouldDeleteCategory() throws Exception {
        // Given
        doNothing().when(categoryService).deleteCategory(categoryId);

        // When & Then
        mockMvc.perform(delete("/v1/categories/{id}", categoryId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(categoryId);
    }
}
