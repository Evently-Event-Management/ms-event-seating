package com.ticketly.mseventseating.service.category;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Category;
import com.ticketly.mseventseating.repository.CategoryRepository;
import dto.projection.CategoryProjectionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryProjectionDataService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public CategoryProjectionDTO getCategoryProjectionData(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found for projection: " + categoryId));

        return mapToProjectionDTO(category);
    }

    private CategoryProjectionDTO mapToProjectionDTO(Category category) {
        Category parent = category.getParent();
        return CategoryProjectionDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(parent != null ? parent.getId() : null)
                .parentName(parent != null ? parent.getName() : null)
                .build();
    }
}
