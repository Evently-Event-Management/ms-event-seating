package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationOwnershipService {

    private final OrganizationRepository organizationRepository;
    // ✅ Inject the RedisTemplate to interact with Redis directly
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Check if a user owns an organization, with the result cached.
     * This method correctly caches a boolean value.
     */
    @Cacheable(value = "organizationOwnership", key = "'ownership_' + #userId + '_' + #organizationId")
    public boolean isOrganizationOwnedByUser(String userId, UUID organizationId) {
        log.debug("--- DATABASE HIT: Checking ownership for org {} and user {} ---", organizationId, userId);
        return organizationRepository.findById(organizationId)
                .map(organization -> organization.getUserId().equals(userId))
                .orElse(false);
    }

    /**
     * Evict a specific organization ownership cache entry.
     * This works perfectly with the standard annotation.
     */
    @CacheEvict(value = "organizationOwnership", key = "'ownership_' + #userId + '_' + #organizationId")
    public void evictOrganizationOwnershipCache(String userId, UUID organizationId) {
        log.info("Evicting organization ownership cache for user {} and organization {}", userId, organizationId);
    }

    /**
     * Evict all organization ownership cache entries for a specific organization.
     * This requires direct Redis interaction.
     */
    public void evictAllOwnershipByOrganization(UUID organizationId) {
        log.info("Evicting all ownership cache entries for organization {}", organizationId);
        // ✅ Construct the pattern to match all keys for this organization
        String pattern = "organizationOwnership::ownership_*_" + organizationId;
        Set<String> keys = redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} keys for pattern: {}", keys.size(), pattern);
        }
    }

    /**
     * Evict all organization ownership cache entries.
     * This is a powerful operation and should be used with care.
     */
    @CacheEvict(value = "organizationOwnership", allEntries = true)
    public void evictAllOrganizationOwnershipCache() {
        log.info("Evicting all organization ownership cache entries.");
    }
}
