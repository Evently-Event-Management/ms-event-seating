package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.CategoryRequest;
import com.ticketly.mseventseating.dto.CategoryResponse;
import com.ticketly.mseventseating.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Get all categories - accessible to all authenticated users
     *
     * @return list of all categories with their hierarchies
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Get a category by ID - accessible to all authenticated users
     *
     * @param id the category ID
     * @return the category if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    /**
     * Create a new category - only accessible to users with manage_categories role
     *
     * @param request the category data
     * @return the created category
     */
    @PostMapping
    @PreAuthorize("hasRole('manage_categories')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        log.info("Creating new category with name: {}", request.getName());
        CategoryResponse createdCategory = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    /**
     * Update a category - only accessible to users with manage_categories role
     *
     * @param id the category ID to update
     * @param request the updated category data
     * @return the updated category
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('manage_categories')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        log.info("Updating category with id: {}", id);
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    /**
     * Delete a category - only accessible to users with manage_categories role
     *
     * @param id the category ID to delete
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('manage_categories')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        log.info("Deleting category with id: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
