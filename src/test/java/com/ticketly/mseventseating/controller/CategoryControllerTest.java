package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.category.CategoryRequest;
import com.ticketly.mseventseating.dto.category.CategoryResponse;
import com.ticketly.mseventseating.service.category.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private UUID categoryId;
    private CategoryRequest validRequest;
    private CategoryResponse mockResponse;
    private List<CategoryResponse> mockResponses;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        UUID parentCategoryId = UUID.randomUUID();

        // Set up a valid category request
        validRequest = CategoryRequest.builder()
                .name("Test Category")
                .parentId(parentCategoryId) // Optional - can be null for top-level categories
                .build();

        // Set up a mock child category response
        CategoryResponse childCategoryResponse = CategoryResponse.builder()
                .id(UUID.randomUUID())
                .name("Child Category")
                .parentId(categoryId)
                .subCategories(new HashSet<>())
                .build();

        // Set up a mock category response
        mockResponse = CategoryResponse.builder()
                .id(categoryId)
                .name("Test Category")
                .parentId(parentCategoryId)
                .subCategories(new HashSet<>(Collections.singletonList(childCategoryResponse)))
                .build();

        // Create a list of mock responses
        mockResponses = Collections.singletonList(mockResponse);
    }

    @Test
    void getAllCategories_ShouldReturnCategories() {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(mockResponses);

        // Act
        ResponseEntity<List<CategoryResponse>> response = categoryController.getAllCategories();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponses, response.getBody());
        verify(categoryService).getAllCategories();
    }

    @Test
    void getCategoryById_ShouldReturnCategory() {
        // Arrange
        when(categoryService.getCategoryById(categoryId)).thenReturn(mockResponse);

        // Act
        ResponseEntity<CategoryResponse> response = categoryController.getCategoryById(categoryId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(categoryService).getCategoryById(categoryId);
    }

    @Test
    void createCategory_ShouldReturnCreatedCategory() {
        // Arrange
        when(categoryService.createCategory(validRequest)).thenReturn(mockResponse);

        // Act
        ResponseEntity<CategoryResponse> response = categoryController.createCategory(validRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(categoryService).createCategory(validRequest);
    }

    @Test
    void updateCategory_ShouldReturnUpdatedCategory() {
        // Arrange
        when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<CategoryResponse> response = categoryController.updateCategory(categoryId, validRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
    }

    @Test
    void deleteCategory_ShouldReturnNoContent() {
        // Arrange
        doNothing().when(categoryService).deleteCategory(categoryId);

        // Act
        ResponseEntity<Void> response = categoryController.deleteCategory(categoryId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(categoryService).deleteCategory(categoryId);
    }
}
