package com.ticketly.mseventseating.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.layout_template.LayoutDataDTO;
import com.ticketly.mseventseating.dto.layout_template.SeatingLayoutTemplateDTO;
import com.ticketly.mseventseating.dto.layout_template.SeatingLayoutTemplateRequest;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.SeatingLayoutTemplate;
import com.ticketly.mseventseating.model.SubscriptionLimitType;
import com.ticketly.mseventseating.repository.SeatingLayoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final ObjectMapper objectMapper;
    private final OrganizationOwnershipService ownershipService;
    private final SeatingLayoutTemplateOwnershipService templateOwnershipService;
    private final LimitService limitService;
    private final OrganizationService organizationService;

    private int getGap() {
        return limitService.getSeatingLayoutConfig().getDefaultGap();
    }

    private int getDefaultPageSize() {
        return limitService.getSeatingLayoutConfig().getDefaultPageSize();
    }

    /**
     * Get all templates for an organization with pagination.
     */
    @Transactional(readOnly = true)
    public Page<SeatingLayoutTemplateDTO> getAllTemplatesByOrganizationId(
            UUID organizationId,
            String userId,
            int page,
            int size) {

        // Verify the user owns the organization
        if (!ownershipService.isOwner(organizationId, userId)) {
            throw new AuthorizationDeniedException("You don't have permission to access this organization's templates");
        }

        if (size <= 0) {
            size = getDefaultPageSize();
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());

        log.info("Fetching templates for organization {} (page: {}, size: {})", organizationId, page, size);
        return seatingLayoutTemplateRepository.findByOrganizationId(organizationId, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Get a template by its ID.
     */
    @Transactional(readOnly = true)
    public SeatingLayoutTemplateDTO getTemplateById(UUID id, String userId) {
        // Check if the user is the owner of the template's organization
        if (!templateOwnershipService.isOwner(id, userId)) {
            throw new AuthorizationDeniedException("You don't have permission to access this template");
        }

        SeatingLayoutTemplate template = findTemplateById(id);
        return convertToDTO(template);
    }

    /**
     * Create a new template, checking tier limits.
     */
    @Transactional
    public SeatingLayoutTemplateDTO createTemplate(SeatingLayoutTemplateRequest request, String userId, Jwt jwt) {
        // Get the organization
        Organization organization = organizationService.verifyOwnershipAndGetOrganization(request.getOrganizationId(), userId);

        long currentTemplateCount = seatingLayoutTemplateRepository.countByOrganizationId(organization.getId());
        int maxTemplates = limitService.getTierLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG, jwt);

        if (currentTemplateCount >= maxTemplates) {
            throw new BadRequestException(String.format(
                    "You have reached the maximum limit of %d seating layouts for your tier.",
                    maxTemplates
            ));
        }

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
     * Update an existing template.
     */
    @Transactional
    public SeatingLayoutTemplateDTO updateTemplate(UUID id, SeatingLayoutTemplateRequest request, String userId) {
        // Check if the user is the owner of the template
        if (!templateOwnershipService.isOwner(id, userId)) {
            throw new AuthorizationDeniedException("You don't have permission to update this template");
        }

        SeatingLayoutTemplate template = findTemplateById(id);

        // Verify the requested organization ID matches the template's current organization ID.
        if (!template.getOrganization().getId().equals(request.getOrganizationId())) {
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
     * Delete a template.
     */
    @Transactional
    public void deleteTemplate(UUID id, String userId) {
        // Check if the user is the owner of the template
        if (!templateOwnershipService.isOwner(id, userId)) {
            throw new AuthorizationDeniedException("You don't have permission to delete this template");
        }

        SeatingLayoutTemplate template = findTemplateById(id);
        seatingLayoutTemplateRepository.delete(template);

        // Evict the template from cache
        templateOwnershipService.evictTemplateCacheById(id);
    }

    private SeatingLayoutTemplate findTemplateById(UUID id) {
        return seatingLayoutTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seating layout template not found with ID: " + id));
    }

    private LayoutDataDTO normalizeLayoutData(LayoutDataDTO layoutData) {
        if (layoutData == null || layoutData.getLayout() == null || layoutData.getLayout().getBlocks() == null || layoutData.getLayout().getBlocks().isEmpty()) {
            return layoutData;
        }

        double minX = layoutData.getLayout().getBlocks().stream()
                .mapToDouble(block -> block.getPosition().getX())
                .min().orElse(0.0);
        double minY = layoutData.getLayout().getBlocks().stream()
                .mapToDouble(block -> block.getPosition().getY())
                .min().orElse(0.0);

        LayoutDataDTO normalized = new LayoutDataDTO();
        normalized.setName(layoutData.getName());
        LayoutDataDTO.Layout normalizedLayout = new LayoutDataDTO.Layout();

        List<LayoutDataDTO.Block> normalizedBlocks = layoutData.getLayout().getBlocks().stream()
                .map(block -> {
                    LayoutDataDTO.Block newBlock = new LayoutDataDTO.Block();
                    newBlock.setId(UUID.randomUUID().toString());
                    newBlock.setName(block.getName());
                    newBlock.setType(block.getType());

                    LayoutDataDTO.Position normalizedPosition = new LayoutDataDTO.Position();
                    normalizedPosition.setX(block.getPosition().getX() - minX + getGap());
                    normalizedPosition.setY(block.getPosition().getY() - minY + getGap());
                    newBlock.setPosition(normalizedPosition);

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
        }
        return dto;
    }
}
