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

    @Transactional(readOnly = true)
    public List<SeatingLayoutTemplateDTO> getAllTemplatesByOrganizationId(UUID organizationId, String userId) {
        // Verify organization exists and user has access to it
        findOrganizationByIdAndVerifyAccess(organizationId, userId);

        return seatingLayoutTemplateRepository.findByOrganizationId(organizationId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SeatingLayoutTemplateDTO getTemplateById(UUID id, String userId) {
        SeatingLayoutTemplate template = seatingLayoutTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seating layout template not found with ID: " + id));
        
        // Verify user has access to the organization that owns this template
        verifyUserAccess(template.getOrganization(), userId);
        
        return convertToDTO(template);
    }

    @Transactional
    public SeatingLayoutTemplateDTO createTemplate(SeatingLayoutTemplateRequest request, String userId) {
        // Verify organization exists and user has access to it
        Organization organization = findOrganizationByIdAndVerifyAccess(request.getOrganizationId(), userId);

        // Process layout data - normalize coordinates and replace IDs
        LayoutDataDTO normalizedLayout = normalizeLayoutData(request.getLayoutData());

        // Convert to entity and save
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
        return convertToDTO(saved);
    }

    @Transactional
    public SeatingLayoutTemplateDTO updateTemplate(UUID id, SeatingLayoutTemplateRequest request, String userId) {
        // Verify template exists
        SeatingLayoutTemplate template = seatingLayoutTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seating layout template not found with ID: " + id));

        // Verify user has access to the organization that owns this template
        verifyUserAccess(template.getOrganization(), userId);

        // Verify organization exists and user has access to it
        Organization organization = findOrganizationByIdAndVerifyAccess(request.getOrganizationId(), userId);

        if (!template.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Cannot change the organization of an existing template");
        }

        // Process layout data - normalize coordinates and replace IDs
        LayoutDataDTO normalizedLayout = normalizeLayoutData(request.getLayoutData());

        // Update template
        template.setName(request.getName());

        try {
            template.setLayoutData(objectMapper.writeValueAsString(normalizedLayout));
        } catch (JsonProcessingException e) {
            log.error("Error serializing layout data", e);
            throw new RuntimeException("Error processing layout data", e);
        }

        SeatingLayoutTemplate saved = seatingLayoutTemplateRepository.save(template);
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteTemplate(UUID id, String userId) {
        SeatingLayoutTemplate template = seatingLayoutTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seating layout template not found with ID: " + id));
        
        // Verify user has access to the organization that owns this template
        verifyUserAccess(template.getOrganization(), userId);
        
        seatingLayoutTemplateRepository.deleteById(id);
    }

    /**
     * Helper method to find an organization by ID and verify the user has access to it
     *
     * @param organizationId the organization ID
     * @param userId the ID of the current user
     * @return the organization entity
     * @throws ResourceNotFoundException if organization not found
     * @throws AuthorizationDeniedException if user does not have access to the organization
     */
    private Organization findOrganizationByIdAndVerifyAccess(UUID organizationId, String userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));
        
        verifyUserAccess(organization, userId);
        return organization;
    }
    
    /**
     * Verify that the user has access to the organization
     *
     * @param organization the organization to check
     * @param userId the ID of the current user
     * @throws AuthorizationDeniedException if user does not have access to the organization
     */
    private void verifyUserAccess(Organization organization, String userId) {
        if (!organization.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("You do not have permission to access this resource");
        }
    }

    /**
     * Normalizes the layout data by:
     * 1. Replacing all block IDs with UUIDs
     * 2. Normalizing coordinates so that leftmost x is 0 and topmost y is 0
     */
    private LayoutDataDTO normalizeLayoutData(LayoutDataDTO layoutData) {
        if (layoutData == null || layoutData.getLayout() == null || layoutData.getLayout().getBlocks() == null) {
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
                    normalizedPosition.setX(block.getPosition().getX() - minX);
                    normalizedPosition.setY(block.getPosition().getY() - minY);
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
