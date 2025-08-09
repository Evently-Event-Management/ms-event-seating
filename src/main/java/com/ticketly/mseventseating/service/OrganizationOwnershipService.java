package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationOwnershipService {

    private final OrganizationRepository organizationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Finds an organization by its ID and verifies that the given user is the owner.
     * The result (the Organization object) is cached.
     * Throws an exception if the organization is not found or if the user is not the owner.
     *
     * @param organizationId the organization ID
     * @param userId         the user ID
     * @return the Organization entity if checks pass
     * @throws ResourceNotFoundException    if the organization does not exist
     * @throws AuthorizationDeniedException if the user does not own the organization
     */
    @Cacheable(value = "organizations", key = "#organizationId + '-' + #userId")
    public Organization verifyOwnershipAndGetOrganization(UUID organizationId, String userId) {
        log.info("--- DATABASE HIT: Verifying ownership and fetching org ID: {} ---", organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        if (!organization.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("User does not have access to this organization");
        }

        return organization;
    }

    /**
     * Evicts the organization cache for a specific organization ID.
     * This is useful when an organization is updated or deleted, ensuring that the cache is refreshed.
     *
     * @param organizationId the ID of the organization to evict from the cache
     */
    public void evictOrganizationCacheById(UUID organizationId) {
        Set<String> keys = redisTemplate.keys("event-seating-ms::organizations::" + organizationId + "-*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Evicted organization cache for ID: {}", organizationId);
        } else {
            log.debug("No cache entries found for organization ID: {}", organizationId);
        }
    }
}
