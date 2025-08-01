package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationOwnershipService {

    private final OrganizationRepository organizationRepository;

    /**
     * Check if a user owns an organization, with result cached
     * Using a simple string key to avoid serialization issues
     *
     * @param userId        the user ID
     * @param organizationId the organization ID
     * @return true if the user owns the organization, false otherwise
     */
    @Cacheable(value = "organizationOwnership", key = "'ownership_' + #userId + '_' + #organizationId")
    public boolean isOrganizationOwnedByUser(String userId, UUID organizationId) {
        log.debug("--- DATABASE HIT: Checking ownership for org {} and user {} ---", organizationId, userId);
        boolean result = organizationRepository.findById(organizationId)
                .map(organization -> organization.getUserId().equals(userId))
                .orElse(false);
        log.debug("--- Cache miss result for {} and {}: {} ---", userId, organizationId, result);
        return result;
    }

    /**
     * Evict specific organization ownership cache entry
     */
    @CacheEvict(value = "organizationOwnership", key = "'ownership_' + #userId + '_' + #organizationId")
    public void evictOrganizationOwnershipCache(String userId, UUID organizationId) {
        log.info("Evicting organization ownership cache for user {} and organization {}", userId, organizationId);
    }

    /**
     * Evict all organization ownership cache entries for a user
     */
    @CacheEvict(value = "organizationOwnership", key = "'ownership_' + #userId + '_*'")
    public void evictAllUserOrganizationOwnershipCache(String userId) {
        log.info("Evicting all organization ownership cache entries for user {}", userId);
    }

    /**
     * Evict all organization ownership cache entries for a specific organization
     */
    @CacheEvict(value = "organizationOwnership", key = "'ownership_*_' + #organizationId")
    public void evictOrganizationOwnershipCacheByOrganization(UUID organizationId) {
        log.info("Evicting all organization ownership cache entries for organization {}", organizationId);
    }


    /**
     * Evict all organization ownership cache entries for an organization
     */
    @CacheEvict(value = "organizationOwnership", allEntries = true)
    public void evictAllOrganizationOwnershipCache() {
        log.info("Evicting all organization ownership cache entries.");
    }
}
