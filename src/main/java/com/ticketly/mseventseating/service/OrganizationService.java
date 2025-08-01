package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.dto.OrganizationRequest;
import com.ticketly.mseventseating.dto.OrganizationResponse;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import org.springframework.security.authorization.AuthorizationDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final S3StorageService s3StorageService;
    private final OrganizationOwnershipService ownershipService;

    @Value("${app.organization.max-per-user:3}")
    private int maxOrganizationsPerUser;

    @Value("${app.organization.max-logo-size}")
    private long maxLogoSize;

    /**
     * Get all organizations for the current user
     *
     * @param userId the ID of the current user
     * @return List of organization responses
     */
    public List<OrganizationResponse> getAllOrganizationsForUser(String userId) {
        return organizationRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get an organization by ID, ensuring the requesting user is the owner
     *
     * @param id     the organization ID
     * @param userId the ID of the requesting user
     * @return the organization response
     * @throws AuthorizationDeniedException if the organization does not exist or user is not owner
     */
    public OrganizationResponse getOrganizationById(UUID id, String userId) {
        Organization organization = findOrganizationByIdAndUser(id, userId);
        return mapToDto(organization);
    }

    /**
     * Create a new organization for the current user
     *
     * @param request the organization request
     * @param userId  the ID of the current user
     * @return the created organization response
     * @throws BadRequestException if the user has reached the maximum organization limit
     */
    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request, String userId) {
        // Check if user has reached the max organization limit
        long userOrganizationCount = organizationRepository.countByUserId(userId);
        if (userOrganizationCount >= maxOrganizationsPerUser) {
            throw new BadRequestException("You have reached the maximum limit of " +
                    maxOrganizationsPerUser + " organizations per user");
        }

        Organization organization = Organization.builder()
                .name(request.getName())
                .website(request.getWebsite())
                .userId(userId)
                .build();

        Organization savedOrganization = organizationRepository.save(organization);
        log.info("Created new organization with ID: {}", savedOrganization.getId());

        return mapToDto(savedOrganization);
    }

    /**
     * Update an existing organization
     *
     * @param id      the organization ID
     * @param request the updated organization details
     * @param userId  the ID of the current user
     * @return the updated organization response
     * @throws AuthorizationDeniedException if the organization does not exist or user is not owner
     */
    @Transactional
    public OrganizationResponse updateOrganization(UUID id, OrganizationRequest request, String userId) {
        Organization organization = findOrganizationByIdAndUser(id, userId);

        organization.setName(request.getName());
        organization.setWebsite(request.getWebsite());

        Organization updatedOrganization = organizationRepository.save(organization);

        // Remember to evict the cache if ownership changes

        log.info("Updated organization with ID: {}", updatedOrganization.getId());
        return mapToDto(updatedOrganization);
    }

    /**
     * Upload a logo for an organization
     *
     * @param id       the organization ID
     * @param logoFile the logo file to upload
     * @param userId   the ID of the current user
     * @return the updated organization response
     * @throws AuthorizationDeniedException if the organization does not exist or user is not owner
     * @throws IOException                  if there is an error handling the file
     */
    @Transactional
    public OrganizationResponse uploadLogo(UUID id, MultipartFile logoFile, String userId) throws IOException {
        // Validate file type
        if (logoFile.isEmpty() || !Objects.requireNonNull(logoFile.getContentType()).startsWith("image/")) {
            throw new BadRequestException("Invalid file type. Please upload an image file.");
        }
        // Validate file size
        if (logoFile.getSize() > maxLogoSize) {
            throw new BadRequestException("File size exceeds the maximum allowed size of " +
                    (maxLogoSize / (1024 * 1024)) + "MB");
        }

        Organization organization = findOrganizationByIdAndUser(id, userId);

        // Delete old logo if it exists
        if (organization.getLogoUrl() != null) {
            s3StorageService.deleteFile(organization.getLogoUrl());
        }

        // Upload new logo
        String logoKey = s3StorageService.uploadFile(logoFile, "organization-logos");
        organization.setLogoUrl(logoKey);

        Organization updatedOrganization = organizationRepository.save(organization);
        log.info("Uploaded logo for organization with ID: {}", updatedOrganization.getId());

        return mapToDto(updatedOrganization);
    }

    /**
     * Remove the logo for an organization
     *
     * @param id     the organization ID
     * @param userId the ID of the current user
     */
    @Transactional
    public void removeLogo(UUID id, String userId) {
        Organization organization = findOrganizationByIdAndUser(id, userId);

        if (organization.getLogoUrl() != null) {
            s3StorageService.deleteFile(organization.getLogoUrl());
            organization.setLogoUrl(null);
            organizationRepository.save(organization);
            log.info("Removed logo for organization with ID: {}", id);
        }
    }


    /**
     * Delete an organization
     *
     * @param id     the organization ID
     * @param userId the ID of the current user
     * @throws AuthorizationDeniedException if the organization does not exist or user is not owner
     */
    @Transactional
    public void deleteOrganization(UUID id, String userId) {
        Organization organization = findOrganizationByIdAndUser(id, userId);

        if (organization.getLogoUrl() != null) {
            s3StorageService.deleteFile(organization.getLogoUrl());
        }

        organizationRepository.delete(organization);

        ownershipService.evictOrganizationOwnershipCache(userId, id);

        log.info("Deleted organization with ID: {}", id);
    }

    /**
     * Helper method to find an organization by ID and verify the user is the owner
     *
     * @param id     the organization ID
     * @param userId the ID of the current user
     * @return the organization entity
     * @throws AuthorizationDeniedException if the organization does not exist or user is not owner
     */
    private Organization findOrganizationByIdAndUser(UUID id, String userId) {
        if (!ownershipService.isOrganizationOwnedByUser(userId, id)) {
            throw new AuthorizationDeniedException("Organization not found or you don't have permission to access it");
        }

        // Since we know the user is authorized, we can safely fetch the entity.
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + id));
    }

    /**
     * Map organization entity to DTO
     *
     * @param organization the organization entity
     * @return the organization response DTO
     */
    private OrganizationResponse mapToDto(Organization organization) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .logoUrl(organization.getLogoUrl() != null ?
                        s3StorageService.generatePresignedUrl(organization.getLogoUrl(), 60) : null)
                .website(organization.getWebsite())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }
}
