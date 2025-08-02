package com.ticketly.mseventseating.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.LayoutDataDTO;
import com.ticketly.mseventseating.dto.SeatingLayoutTemplateDTO;
import com.ticketly.mseventseating.dto.SeatingLayoutTemplateRequest;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.SeatingLayoutTemplate;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import com.ticketly.mseventseating.repository.SeatingLayoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatingLayoutTemplateService {

    private final SeatingLayoutTemplateRepository seatingLayoutTemplateRepository;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;
    private final OrganizationOwnershipService ownershipService;

    @Value("${app.seating_layout.default-gap:25}")
    private int gap;

    @Value("${app.pagination.default-size:6}")
    private int defaultPageSize;

    /**
     * Get all templates for an organization with caching and pagination
     *
     * @param organizationId the organization ID
     * @param userId the ID of the current user
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated list of seating layout template DTOs
     */
    @Transactional(readOnly = true)
    public Page<SeatingLayoutTemplateDTO> getAllTemplatesByOrganizationId(
            UUID organizationId,
            String userId,
            int page,
            int size) {
        // It still uses the cached ownership check for authorization.
        verifyUserAccess(organizationId, userId);

        // Use default page size if 0 or negative size is provided
        if (size <= 0) {
            size = defaultPageSize;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());

        log.info("Fetching templates for organization {} (page: {}, size: {})", organizationId, page, size);
        return seatingLayoutTemplateRepository.findByOrganizationId(organizationId, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Get a template by ID
     *
     * @param id the template ID
     * @param userId the ID of the current user
     * @return the seating layout template DTO
     */
    @Transactional(readOnly = true)
    public SeatingLayoutTemplateDTO getTemplateById(UUID id, String userId) {
        SeatingLayoutTemplate template = findTemplateById(id);
        verifyUserAccess(template.getOrganization().getId(), userId);
        return convertToDTO(template);
    }

    /**
     * Create a new template
     *
     * @param request the create template request
     * @param userId the ID of the current user
     * @return the created seating layout template DTO
     */
    @Transactional
    public SeatingLayoutTemplateDTO createTemplate(SeatingLayoutTemplateRequest request, String userId) {
        Organization organization = findOrganizationByIdAndVerifyAccess(request.getOrganizationId(), userId);
        LayoutDataDTO normalizedLayout = normalizeLayoutData(request.getLayoutData());

        SeatingLayoutTemplate template = new SeatingLayoutTemplate();
        template.setName(request.getName());
        template.setOrganization(organization);

        try {
            template.setLayoutData(objectMapper.writeValueAsString(normalizedLayout));
        } catch (JsonProcessingException e) {
            log.error("Error serializing layout data", e);
            throw new RuntimeException("Error processing layout data", e);
        }

        SeatingLayoutTemplate saved = seatingLayoutTemplateRepository.save(template);
        log.debug("Created seating layout template with ID: {} for organization: {}",
                saved.getId(), organization.getId());

        return convertToDTO(saved);
    }

    /**
     * Update an existing template
     *
     * @param id the template ID
     * @param request the update template request
     * @param userId the ID of the current user
     * @return the updated seating layout template DTO
     */
    @Transactional
    public SeatingLayoutTemplateDTO updateTemplate(UUID id, SeatingLayoutTemplateRequest request, String userId) {
        SeatingLayoutTemplate template = findTemplateById(id);
        verifyUserAccess(template.getOrganization().getId(), userId);
        Organization organization = findOrganizationByIdAndVerifyAccess(request.getOrganizationId(), userId);

        if (!template.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Cannot change the organization of an existing template");
        }

        LayoutDataDTO normalizedLayout = normalizeLayoutData(request.getLayoutData());
        template.setName(request.getName());

        try {
            template.setLayoutData(objectMapper.writeValueAsString(normalizedLayout));
        } catch (JsonProcessingException e) {
            log.error("Error serializing layout data", e);
            throw new RuntimeException("Error processing layout data", e);
        }

        SeatingLayoutTemplate saved = seatingLayoutTemplateRepository.save(template);
        log.debug("Updated seating layout template with ID: {}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Delete a template
     *
     * @param id the template ID
     * @param userId the ID of the current user
     */
    @Transactional
    public void deleteTemplate(UUID id, String userId) {
        SeatingLayoutTemplate template = findTemplateById(id);
        verifyUserAccess(template.getOrganization().getId(), userId);
        seatingLayoutTemplateRepository.delete(template);
    }

    /**
     * Helper method to find an organization by ID and verify the user has access to it
     * This method uses cached ownership check to improve performance
     *
     * @param organizationId the organization ID
     * @param userId the ID of the current user
     * @return the organization entity
     * @throws ResourceNotFoundException if organization not found
     * @throws AuthorizationDeniedException if user does not have access to the organization
     */
    private Organization findOrganizationByIdAndVerifyAccess(UUID organizationId, String userId) {
        // Use cached ownership check first
        if (!ownershipService.isOrganizationOwnedByUser(userId, organizationId)) {
            throw new AuthorizationDeniedException("Organization not found or you don't have permission to access it");
        }

        // If ownership check passes, retrieve the organization
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));
    }

    /**
     * Verify that the user has access to the organization using cached check
     *
     * @param organizationId the organization to check
     * @param userId the ID of the current user
     * @throws AuthorizationDeniedException if user does not have access to the organization
     */
    private void verifyUserAccess(UUID organizationId, String userId) {
        if (!ownershipService.isOrganizationOwnedByUser(userId, organizationId)) {
            throw new AuthorizationDeniedException("You do not have permission to access this resource");
        }
    }

    private SeatingLayoutTemplate findTemplateById(UUID id) {
        return seatingLayoutTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seating layout template not found with ID: " + id));
    }

    /**
     * Normalizes the layout data by:
     * 1. Replacing all block IDs with UUIDs
     * 2. Normalizing coordinates so that the layout starts near the top-left corner with a small margin.
     */
    private LayoutDataDTO normalizeLayoutData(LayoutDataDTO layoutData) {
        if (layoutData == null || layoutData.getLayout() == null || layoutData.getLayout().getBlocks() == null || layoutData.getLayout().getBlocks().isEmpty()) {
            return layoutData;
        }

        // Find minimum x and y values
        double minX = layoutData.getLayout().getBlocks().stream()
                .mapToDouble(block -> block.getPosition().getX())
                .min()
                .orElse(0.0);

        double minY = layoutData.getLayout().getBlocks().stream()
                .mapToDouble(block -> block.getPosition().getY())
                .min()
                .orElse(0.0);

        // Create a new LayoutDataDTO with normalized blocks
        LayoutDataDTO normalized = new LayoutDataDTO();
        normalized.setName(layoutData.getName());

        LayoutDataDTO.Layout normalizedLayout = new LayoutDataDTO.Layout();

        List<LayoutDataDTO.Block> normalizedBlocks = layoutData.getLayout().getBlocks().stream()
                .map(block -> {
                    // Create a new block with a UUID
                    LayoutDataDTO.Block newBlock = new LayoutDataDTO.Block();
                    newBlock.setId(UUID.randomUUID().toString());
                    newBlock.setName(block.getName());
                    newBlock.setType(block.getType());

                    // Normalize position
                    LayoutDataDTO.Position normalizedPosition = new LayoutDataDTO.Position();
                    normalizedPosition.setX(block.getPosition().getX() - minX + gap);
                    normalizedPosition.setY(block.getPosition().getY() - minY + gap);
                    newBlock.setPosition(normalizedPosition);

                    // Copy other properties based on block type
                    if ("seated_grid".equals(block.getType())) {
                        newBlock.setRows(block.getRows());
                        newBlock.setColumns(block.getColumns());
                        newBlock.setStartRowLabel(block.getStartRowLabel());
                        newBlock.setStartColumnLabel(block.getStartColumnLabel());
                    } else {
                        newBlock.setWidth(block.getWidth());
                        newBlock.setHeight(block.getHeight());

                        if ("standing_capacity".equals(block.getType())) {
                            newBlock.setCapacity(block.getCapacity());
                        }
                    }

                    return newBlock;
                })
                .collect(Collectors.toList());

        normalizedLayout.setBlocks(normalizedBlocks);
        normalized.setLayout(normalizedLayout);

        return normalized;
    }

    private SeatingLayoutTemplateDTO convertToDTO(SeatingLayoutTemplate template) {
        SeatingLayoutTemplateDTO dto = new SeatingLayoutTemplateDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setOrganizationId(template.getOrganization().getId());
        dto.setUpdatedAt(template.getUpdatedAt());

        try {
            LayoutDataDTO layoutData = objectMapper.readValue(template.getLayoutData(), LayoutDataDTO.class);
            dto.setLayoutData(layoutData);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing layout data for template ID: {}", template.getId(), e);
            // We'll still return the DTO, just without the layout data
        }

        return dto;
    }
}