package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.category.CategoryRequest;
import com.ticketly.mseventseating.dto.category.CategoryResponse;
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
     * Retrieve all categories with their subcategory hierarchies.
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        log.info("Fetching all categories with hierarchy");
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Retrieve a single category by its ID, including its subcategories.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable UUID id) {
        log.info("Fetching category by id: {}", id);
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    /**
     * Create a new category. Only users with 'category_admin' role are allowed.
     */
    @PostMapping
    @PreAuthorize("hasRole('category_admin')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        log.info("Request to create category: {}", request.getName());
        CategoryResponse createdCategory = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    /**
     * Update an existing category. Only users with 'category_admin' role are allowed.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('category_admin')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        log.info("Request to update category id: {} with name: {}", id, request.getName());
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    /**
     * Delete a category by its ID. Only users with 'category_admin' role are allowed.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('category_admin')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        log.info("Request to delete category id: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
