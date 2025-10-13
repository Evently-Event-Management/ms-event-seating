package com.ticketly.mseventseating.service.organization;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.OrganizationMember;
import com.ticketly.mseventseating.model.OrganizationRole;
import com.ticketly.mseventseating.repository.OrganizationMemberRepository;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationOwnershipService {

    private final OrganizationRepository organizationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrganizationMemberRepository memberRepository;

    /**
     * Verifies that a user is the owner of an organization.
     * The boolean result of this check is cached.
     *
     * @param organizationId the organization ID
     * @param userId         the user ID
     * @return true if the user is the owner
     * @throws ResourceNotFoundException if the organization does not exist
     */
    @Cacheable(value = "organizationOwnership", key = "#organizationId + '-' + #userId")
    @Transactional(readOnly = true)
    public boolean isOwner(UUID organizationId, String userId) {
        log.info("--- DATABASE HIT: Verifying organization ownership for org ID: {} ---", organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        return organization.getUserId().equals(userId);
    }

    /**
     * Checks if a user has a specific role in an organization.
     *
     * @param organizationId the organization ID
     * @param userId         the user ID
     * @param role           the role to check for
     * @return true if the user has the specified role and is active
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "organizationMemberRoles", key = "#organizationId + '-' + #userId + '-' + #role")
    public boolean hasRole(UUID organizationId, String userId, OrganizationRole role) {
        log.info("--- DATABASE HIT: Checking if user {} has role {} in organization {} ---", userId, role, organizationId);

        // Then check for specific role in the set of roles and that the member is active
        Optional<OrganizationMember> member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId);
        return member.isPresent() && member.get().isActive() && member.get().getRoles().contains(role);
    }

    /**
     * Evicts the organization cache for a specific organization ID.
     * This is useful when an organization is updated or deleted, ensuring that the cache is refreshed.
     *
     * @param organizationId the ID of the organization to evict from the cache
     */
    public void evictOrganizationCacheById(UUID organizationId) {
        Set<String> keys = redisTemplate.keys("event-seating-ms::organizationOwnership::" + organizationId + "-*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Evicted organization cache for ID: {}", organizationId);
        } else {
            log.debug("No cache entries found for organization ID: {}", organizationId);
        }
    }

    public void evictOrganizationCacheByUser(UUID userId) {
        Set<String> keys = redisTemplate.keys("event-seating-ms::organizationOwnership::*-" + userId);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Evicted organization cache for user ID: {}", userId);
        } else {
            log.debug("No cache entries found for user ID: {}", userId);
        }
    }

    public void evictMemberRoleCacheByOrganization(UUID organizationId) {
        Set<String> keys = redisTemplate.keys("event-seating-ms::organizationMemberRoles::" + organizationId + "-*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Evicted member role cache for organization ID: {}", organizationId);
        } else {
            log.debug("No member role cache entries found for organization ID: {}", organizationId);
        }
    }


    public void evictMemberRoleCacheByUser(UUID userId) {
        Set<String> keys = redisTemplate.keys("event-seating-ms::organizationMemberRoles::*-" + userId + "-*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Evicted member role cache for user ID: {}", userId);
        } else {
            log.debug("No member role cache entries found for user ID: {}", userId);
        }
    }
}
