package com.ticketly.mseventseating.service.category;

import com.ticketly.mseventseating.dto.category.CategoryRequest;
import com.ticketly.mseventseating.dto.category.CategoryResponse;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Category;
import com.ticketly.mseventseating.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private UUID categoryId;
    private UUID parentCategoryId;
    private Category category;
    private Category parentCategory;
    private CategoryRequest categoryRequest;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        parentCategoryId = UUID.randomUUID();

        // Setup parent category
        parentCategory = Category.builder()
                .id(parentCategoryId)
                .name("Parent Category")
                .subCategories(new HashSet<>())
                .build();

        // Setup category
        category = Category.builder()
                .id(categoryId)
                .name("Test Category")
                .parent(parentCategory)
                .subCategories(new HashSet<>())
                .build();

        parentCategory.getSubCategories().add(category);

        // Setup category request
        categoryRequest = new CategoryRequest();
        categoryRequest.setName("Test Category");
        categoryRequest.setParentId(parentCategoryId);
    }

    @Test
    void getAllCategories_ShouldReturnTopLevelCategories() {
        // Given
        List<Category> topLevelCategories = Collections.singletonList(parentCategory);
        when(categoryRepository.findByParentIsNull()).thenReturn(topLevelCategories);

        // When
        List<CategoryResponse> result = categoryService.getAllCategories();

        // Then
        assertEquals(1, result.size());
        assertEquals(parentCategory.getName(), result.getFirst().getName());
        assertEquals(1, result.getFirst().getSubCategories().size());
        verify(categoryRepository).findByParentIsNull();
    }

    @Test
    void getCategoryById_WithValidId_ShouldReturnCategory() {
        // Given
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // When
        CategoryResponse result = categoryService.getCategoryById(categoryId);

        // Then
        assertNotNull(result);
        assertEquals(category.getId(), result.getId());
        assertEquals(category.getName(), result.getName());
        assertEquals(parentCategoryId, result.getParentId());
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void getCategoryById_WithInvalidId_ShouldThrowException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(categoryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(nonExistentId));
        verify(categoryRepository).findById(nonExistentId);
    }

    @Test
    void createCategory_WithValidRequest_ShouldCreateCategory() {
        // Given
        when(categoryRepository.findByName(categoryRequest.getName())).thenReturn(Optional.empty());
        when(categoryRepository.findById(parentCategoryId)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> {
            Category savedCategory = i.getArgument(0);
            savedCategory.setId(categoryId);
            return savedCategory;
        });

        // When
        CategoryResponse result = categoryService.createCategory(categoryRequest);

        // Then
        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        assertEquals(categoryRequest.getName(), result.getName());
        assertEquals(parentCategoryId, result.getParentId());
        verify(categoryRepository).findByName(categoryRequest.getName());
        verify(categoryRepository).findById(parentCategoryId);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_WithExistingName_ShouldThrowException() {
        // Given
        when(categoryRepository.findByName(categoryRequest.getName())).thenReturn(Optional.of(category));

        // When/Then
        assertThrows(BadRequestException.class, () -> categoryService.createCategory(categoryRequest));
        verify(categoryRepository).findByName(categoryRequest.getName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_WithNonExistentParent_ShouldThrowException() {
        // Given
        when(categoryRepository.findByName(categoryRequest.getName())).thenReturn(Optional.empty());
        when(categoryRepository.findById(parentCategoryId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> categoryService.createCategory(categoryRequest));
        verify(categoryRepository).findByName(categoryRequest.getName());
        verify(categoryRepository).findById(parentCategoryId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_WithValidRequest_ShouldUpdateCategory() {
        // Given
        CategoryRequest updateRequest = new CategoryRequest();
        updateRequest.setName("Updated Category");
        updateRequest.setParentId(null); // Remove parent

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByName(updateRequest.getName())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        CategoryResponse result = categoryService.updateCategory(categoryId, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        assertEquals(updateRequest.getName(), result.getName());
        assertNull(result.getParentId());
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).findByName(updateRequest.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateCategory_WithSameNameDifferentCategory_ShouldThrowException() {
        // Given
        Category otherCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Other Category")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByName(categoryRequest.getName())).thenReturn(Optional.of(otherCategory));

        // When/Then
        assertThrows(BadRequestException.class, () -> categoryService.updateCategory(categoryId, categoryRequest));
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).findByName(categoryRequest.getName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_WithSelfAsParent_ShouldThrowException() {
        // Given
        CategoryRequest updateRequest = new CategoryRequest();
        updateRequest.setName("Updated Category");
        updateRequest.setParentId(categoryId); // Self as parent

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByName(updateRequest.getName())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(BadRequestException.class, () -> categoryService.updateCategory(categoryId, updateRequest));
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).findByName(updateRequest.getName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_WithCircularReference_ShouldThrowException() {
        // Given
        UUID childCategoryId = UUID.randomUUID();
        Category childCategory = Category.builder()
                .id(childCategoryId)
                .name("Child Category")
                .parent(category)
                .build();

        // Add child category to the category's subcategories
        category.getSubCategories().add(childCategory);

        // Create a request that would set the child as the parent (circular)
        CategoryRequest updateRequest = new CategoryRequest();
        updateRequest.setName("Updated Category");
        updateRequest.setParentId(childCategoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(childCategoryId)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.findByName(updateRequest.getName())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(BadRequestException.class, () -> categoryService.updateCategory(categoryId, updateRequest));
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).findByName(updateRequest.getName());
        verify(categoryRepository).findById(childCategoryId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_WithValidId_ShouldDeleteCategory() {
        // Given
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        doNothing().when(categoryRepository).delete(category);

        // When
        categoryService.deleteCategory(categoryId);

        // Then
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_WithInvalidId_ShouldThrowException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(categoryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory(nonExistentId));
        verify(categoryRepository).findById(nonExistentId);
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
