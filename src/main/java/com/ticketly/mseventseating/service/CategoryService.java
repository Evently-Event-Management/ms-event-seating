package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.dto.category.CategoryRequest;
import com.ticketly.mseventseating.dto.category.CategoryResponse;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Category;
import com.ticketly.mseventseating.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Get all categories
     *
     * @return list of all categories
     */
    public List<CategoryResponse> getAllCategories() {
        // Get all top-level categories (without parents)
        List<Category> topLevelCategories = categoryRepository.findByParentIsNull();

        // Map to response DTOs with recursive mapping of subcategories
        return topLevelCategories.stream()
                .map(this::mapToResponseWithSubcategories)
                .collect(Collectors.toList());
    }

    /**
     * Get a category by ID
     *
     * @param id the category ID
     * @return the category if found
     */
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        return mapToResponseWithSubcategories(category);
    }

    /**
     * Create a new category
     *
     * @param request the category request data
     * @return the created category
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        // Check if category with the same name already exists
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .build();

        // Set parent category if parentId is provided
        if (request.getParentId() != null) {
            Category parentCategory = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentId()));
            category.setParent(parentCategory);
        }

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    /**
     * Update an existing category
     *
     * @param id the category ID to update
     * @param request the updated category data
     * @return the updated category
     */
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Check if another category already has this name
        Optional<Category> categoryWithSameName = categoryRepository.findByName(request.getName());
        if (categoryWithSameName.isPresent() && !categoryWithSameName.get().getId().equals(id)) {
            throw new BadRequestException("Another category with name '" + request.getName() + "' already exists");
        }

        // Update name
        existingCategory.setName(request.getName());

        // Update parent if changed
        if (request.getParentId() == null) {
            existingCategory.setParent(null);
        } else if (existingCategory.getParent() == null ||
                   !existingCategory.getParent().getId().equals(request.getParentId())) {
            // Check for circular reference
            if (id.equals(request.getParentId())) {
                throw new BadRequestException("Category cannot be its own parent");
            }

            Category newParent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentId()));

            // Check if the new parent is a descendant of the current category (which would create a cycle)
            if (isDescendant(existingCategory, newParent)) {
                throw new BadRequestException("Cannot set a subcategory as the parent (creates a cycle)");
            }

            existingCategory.setParent(newParent);
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        return mapToResponse(updatedCategory);
    }

    /**
     * Delete a category
     *
     * @param id the category ID to delete
     */
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        categoryRepository.delete(category);
    }

    /**
     * Check if potentialDescendant is a descendant of the ancestor category
     * 
     * @param ancestor The category that should be the ancestor
     * @param potentialDescendant The category to check if it's a descendant
     * @return true if potentialDescendant is a descendant of ancestor, false otherwise
     */
    private boolean isDescendant(Category ancestor, Category potentialDescendant) {
        if (potentialDescendant == null) {
            return false;
        }
        
        // Use a set to keep track of visited categories to avoid infinite loops
        Set<UUID> visited = new HashSet<>();
        return isDescendantHelper(ancestor, potentialDescendant, visited);
    }
    
    private boolean isDescendantHelper(Category ancestor, Category current, Set<UUID> visited) {
        // Base case: if we've already visited this category or it's null
        if (current == null || visited.contains(current.getId())) {
            return false;
        }
        
        // Mark as visited
        visited.add(current.getId());
        
        // Check if the current category is the ancestor
        if (current.getId().equals(ancestor.getId())) {
            return true;
        }
        
        // Recursively check parent
        return isDescendantHelper(ancestor, current.getParent(), visited);
    }

    /**
     * Map a category entity to a response DTO without including subcategories
     */
    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .build();
    }

    /**
     * Map a category entity to a response DTO with recursive mapping of subcategories
     */
    private CategoryResponse mapToResponseWithSubcategories(Category category) {
        Set<CategoryResponse> subCategoryResponses = category.getSubCategories() != null ?
                category.getSubCategories().stream()
                        .map(this::mapToResponseWithSubcategories)
                        .collect(Collectors.toSet()) :
                new HashSet<>();

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .subCategories(subCategoryResponses)
                .build();
    }
}