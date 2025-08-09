package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.dto.organization.OrganizationRequest;
import com.ticketly.mseventseating.dto.organization.OrganizationResponse;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.SubscriptionLimitType;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
     * Get all organizations for the current user.
     *
     * @param userId the ID of the current user
     * @return List of organization responses
     */
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAllOrganizationsForUser(String userId) {
        return organizationRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get an organization by ID, ensuring the requesting user is the owner.
     *
     * @param id     the organization ID
     * @param userId the ID of the requesting user
     * @return the organization response
     */
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(UUID id, String userId) {
        // A single, cached call for verification and retrieval.
        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(id, userId);
        return mapToDto(organization);
    }

    /**
     * Create a new organization for the current user.
     *
     * @param request the organization request
     * @param userId  the ID of the current user
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
        log.info("Created new organization with ID: {}", savedOrganization.getId());
        return mapToDto(savedOrganization);
    }

    /**
     * Update an existing organization. Evicts the cache for this organization.
     *
     * @param id      the organization ID
     * @param request the updated organization details
     * @param userId  the ID of the current user
     * @return the updated organization response
     */
    @Transactional
    @CacheEvict(value = "organizations", key = "#id")
    public OrganizationResponse updateOrganization(UUID id, OrganizationRequest request, String userId) {
        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(id, userId);

        organization.setName(request.getName());
        organization.setWebsite(request.getWebsite());

        Organization updatedOrganization = organizationRepository.save(organization);
        log.info("Updated organization with ID: {}", updatedOrganization.getId());
        ownershipService.evictOrganizationCacheById(id);
        return mapToDto(updatedOrganization);
    }

    /**
     * Upload a logo for an organization. Evicts the cache for this organization.
     *
     * @param id       the organization ID
     * @param logoFile the logo file to upload
     * @param userId   the ID of the current user
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

        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(id, userId);

        if (organization.getLogoUrl() != null) {
            s3StorageService.deleteFile(organization.getLogoUrl());
        }

        String logoKey = s3StorageService.uploadFile(logoFile, "organization-logos");
        organization.setLogoUrl(logoKey);

        Organization updatedOrganization = organizationRepository.save(organization);
        ownershipService.evictOrganizationCacheById(id);
        log.info("Uploaded logo for organization with ID: {}", updatedOrganization.getId());
        return mapToDto(updatedOrganization);
    }

    /**
     * Remove the logo for an organization. Evicts the cache for this organization.
     *
     * @param id     the organization ID
     * @param userId the ID of the current user
     */
    @Transactional
    @CacheEvict(value = "organizations", key = "#id")
    public void removeLogo(UUID id, String userId) {
        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(id, userId);

        if (organization.getLogoUrl() != null) {
            s3StorageService.deleteFile(organization.getLogoUrl());
            organization.setLogoUrl(null);
            organizationRepository.save(organization);
            ownershipService.evictOrganizationCacheById(id);
            log.info("Removed logo for organization with ID: {}", id);
        }
    }

    /**
     * Delete an organization. Evicts the cache for this organization.
     *
     * @param id     the organization ID
     * @param userId the ID of the current user
     */
    @Transactional
    public void deleteOrganization(UUID id, String userId) {
        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(id, userId);

        if (organization.getLogoUrl() != null) {
            s3StorageService.deleteFile(organization.getLogoUrl());
        }

        organizationRepository.delete(organization);
        ownershipService.evictOrganizationCacheById(id);
        log.info("Deleted organization with ID: {}", id);
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
}
