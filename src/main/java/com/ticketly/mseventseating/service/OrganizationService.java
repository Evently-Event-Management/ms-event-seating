package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.dto.organization.OrganizationRequest;
import com.ticketly.mseventseating.dto.organization.OrganizationResponse;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.SubscriptionLimitType;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final LimitService limitService;

    /**
     * Get all organizations owned by the specified user.
     *
     * @param userId the ID of the user
     * @return List of organization responses
     */
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAllOrganizationsForUser(String userId) {
        // No misleading log here, so unchanged.
        return organizationRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get organization details by ID (admin access, no ownership check).
     *
     * @param id the organization ID
     * @return the organization response
     */
    public OrganizationResponse getOrganizationById(UUID id) {
        // No misleading log here, so unchanged.
        return organizationRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
    }

    /**
     * Get organization details by ID for the owner.
     *
     * @param id     the organization ID
     * @param userId the ID of the requesting user
     * @return the organization response
     */
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationByIdOwner(UUID id, String userId) {
        Organization organization = verifyOwnershipAndGetOrganization(id, userId);
        return mapToDto(organization);
    }

    /**
     * Create a new organization for the specified user.
     *
     * @param request the organization request
     * @param userId  the ID of the user
     * @param jwt     the user's JWT for tier checking
     * @return the created organization response
     */
    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request, String userId, Jwt jwt) {
        int maxOrganizations = limitService.getTierLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);
        long userOrganizationCount = organizationRepository.countByUserId(userId);
        if (userOrganizationCount >= maxOrganizations) {
            throw new BadRequestException("You have reached the maximum limit of " +
                    maxOrganizations + " organizations for your tier.");
        }

        Organization organization = Organization.builder()
                .name(request.getName())
                .website(request.getWebsite())
                .userId(userId)
                .build();

        Organization savedOrganization = organizationRepository.save(organization);
        log.info("Organization created. ID: {}, Owner: {}", savedOrganization.getId(), userId);
        return mapToDto(savedOrganization);
    }

    /**
     * Update organization details for the owner. Evicts the cache for this organization.
     *
     * @param id      the organization ID
     * @param request the updated organization details
     * @param userId  the ID of the user
     * @return the updated organization response
     */
    @Transactional
    @CacheEvict(value = "organizations", key = "#id")
    public OrganizationResponse updateOrganization(UUID id, OrganizationRequest request, String userId) {
        Organization organization = verifyOwnershipAndGetOrganization(id, userId);

        organization.setName(request.getName());
        organization.setWebsite(request.getWebsite());

        Organization updatedOrganization = organizationRepository.save(organization);
        log.info("Organization updated. ID: {}, Owner: {}", updatedOrganization.getId(), userId);
        return mapToDto(updatedOrganization);
    }

    /**
     * Upload a logo for the organization. Evicts the cache for this organization.
     *
     * @param id       the organization ID
     * @param logoFile the logo file to upload
     * @param userId   the ID of the user
     * @return the updated organization response
     */
    @Transactional
    @CacheEvict(value = "organizations", key = "#id")
    public OrganizationResponse uploadLogo(UUID id, MultipartFile logoFile, String userId) throws IOException {
        if (logoFile.isEmpty() || !Objects.requireNonNull(logoFile.getContentType()).startsWith("image/")) {
            throw new BadRequestException("Invalid file type. Please upload an image file.");
        }
        long maxLogoSize = getMaxLogoSize();
        if (logoFile.getSize() > maxLogoSize) {
            throw new BadRequestException("File size exceeds the maximum allowed size of " +
                    (maxLogoSize / (1024 * 1024)) + "MB");
        }

        Organization organization = verifyOwnershipAndGetOrganization(id, userId);

        if (organization.getLogoUrl() != null) {
            s3StorageService.deleteFile(organization.getLogoUrl());
        }

        String logoKey = s3StorageService.uploadFile(logoFile, "organization-logos");
        organization.setLogoUrl(logoKey);

        Organization updatedOrganization = organizationRepository.save(organization);
        log.info("Logo uploaded for organization. ID: {}, Owner: {}", updatedOrganization.getId(), userId);
        return mapToDto(updatedOrganization);
    }

    /**
     * Remove the logo from the organization. Evicts the cache for this organization.
     *
     * @param id     the organization ID
     * @param userId the ID of the user
     */
    @Transactional
    @CacheEvict(value = "organizations", key = "#id")
    public void removeLogo(UUID id, String userId) {
        Organization organization = verifyOwnershipAndGetOrganization(id, userId);

        if (organization.getLogoUrl() != null) {
            s3StorageService.deleteFile(organization.getLogoUrl());
            organization.setLogoUrl(null);
            organizationRepository.save(organization);
            log.info("Logo removed from organization. ID: {}, Owner: {}", id, userId);
        }
    }

    /**
     * Delete the organization for the owner. Evicts the cache for this organization.
     *
     * @param id     the organization ID
     * @param userId the ID of the user
     */
    @Transactional
    public void deleteOrganization(UUID id, String userId) {
        Organization organization = verifyOwnershipAndGetOrganization(id, userId);

        if (organization.getLogoUrl() != null) {
            s3StorageService.deleteFile(organization.getLogoUrl());
        }

        organizationRepository.delete(organization);
        ownershipService.evictOrganizationCacheById(id);
        log.info("Organization deleted. ID: {}, Owner: {}", id, userId);
    }

    /**
     * Map organization entity to DTO, generating a presigned URL for the logo if it exists.
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

    // Replace @Value annotation with LimitService
    private long getMaxLogoSize() {
        return limitService.getOrganizationConfig().getMaxLogoSize();
    }

    public Organization verifyOwnershipAndGetOrganization(UUID organizationId, String userId) {
        // First check ownership using the cached method
        if (!ownershipService.isOwner(organizationId, userId)) {
            throw new AuthorizationDeniedException("User does not have access to this organization");
        }

        // If the user is the owner, return the organization
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));
    }
}
