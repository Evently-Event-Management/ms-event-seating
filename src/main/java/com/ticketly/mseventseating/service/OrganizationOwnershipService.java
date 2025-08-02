package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationOwnershipService {

    private final OrganizationRepository organizationRepository;

    /**
     * Finds an organization by its ID and verifies that the given user is the owner.
     * The result (the Organization object) is cached.
     * Throws an exception if the organization is not found or if the user is not the owner.
     *
     * @param organizationId the organization ID
     * @param userId         the user ID
     * @return the Organization entity if checks pass
     * @throws ResourceNotFoundException      if the organization does not exist
     * @throws AuthorizationDeniedException if the user does not own the organization
     */
    @Cacheable(value = "organizations", key = "#organizationId")
    public Organization verifyOwnershipAndGetOrganization(UUID organizationId, String userId) {
        log.info("--- DATABASE HIT: Verifying ownership and fetching org ID: {} ---", organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        if (!organization.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("User does not have access to this organization");
        }

        return organization;
    }

    // Note: The old isOrganizationOwnedByUser and eviction methods can now be removed or refactored
    // as the main OrganizationService will handle eviction on the "organizations" cache.
}
