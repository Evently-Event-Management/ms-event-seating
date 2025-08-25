package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.OrganizationMember;
import com.ticketly.mseventseating.model.OrganizationRole;
import com.ticketly.mseventseating.repository.EventSessionRepository;
import com.ticketly.mseventseating.repository.OrganizationMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service to verify ownership and role-based access to event sessions.
 * Uses caching for better performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionOwnershipService {

    private final EventSessionRepository sessionRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Verifies that a user is the owner of a session's organization.
     * The boolean result of this check is cached.
     *
     * @param sessionId the session ID
     * @param userId    the user ID
     * @return true if the user is the owner
     * @throws ResourceNotFoundException if the session doesn't exist
     */
    @Cacheable(value = "sessionOwnership", key = "#sessionId + '-' + #userId")
    @Transactional(readOnly = true)
    public boolean isOwner(UUID sessionId, String userId) {
        log.info("--- DATABASE HIT: Verifying session ownership for session ID: {} ---", sessionId);
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        // Direct check on the organization's userId
        return session.getEvent().getOrganization().getUserId().equals(userId);
    }

    /**
     * Verifies that a user has the specified role in the session's organization.
     * The boolean result of this check is cached.
     *
     * @param sessionId the session ID
     * @param userId    the user ID
     * @param role      the required role
     * @return true if the user has the specified role
     * @throws ResourceNotFoundException if the session doesn't exist
     */
    @Cacheable(value = "sessionRoleAccess", key = "#sessionId + '-' + #userId + '-' + #role")
    @Transactional(readOnly = true)
    public boolean hasRole(UUID sessionId, String userId, OrganizationRole role) {
        log.info("--- DATABASE HIT: Verifying role {} for user {} in session ID: {} ---", role, userId, sessionId);
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        UUID organizationId = session.getEvent().getOrganization().getId();

        // First check if user is the owner
        if (session.getEvent().getOrganization().getUserId().equals(userId)) {
            return true;
        }

        // Then check for specific role
        Optional<OrganizationMember> member = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userId);
        return member.isPresent() && member.get().getRoles().contains(role);
    }

    /**
     * Evicts the session cache by session ID.
     *
     * @param sessionId the session ID
     */
    public void evictSessionCacheById(UUID sessionId) {
        log.info("Evicting cache for session ID: {}", sessionId);
        Set<String> ownershipKeys = redisTemplate.keys("event-seating-ms::sessionOwnership::" + sessionId + "-*");
        Set<String> roleKeys = redisTemplate.keys("event-seating-ms::sessionRoleAccess::" + sessionId + "-*");

        if (!ownershipKeys.isEmpty()) {
            redisTemplate.delete(ownershipKeys);
            log.debug("Evicted ownership cache keys: {}", ownershipKeys);
        }

        if (!roleKeys.isEmpty()) {
            redisTemplate.delete(roleKeys);
            log.debug("Evicted role access cache keys: {}", roleKeys);
        }
    }

    /**
     * Evicts all session caches for a specific user.
     *
     * @param userId the user ID
     */
    public void evictSessionCacheByUser(String userId) {
        log.info("Evicting all session caches for user ID: {}", userId);
        Set<String> ownershipKeys = redisTemplate.keys("event-seating-ms::sessionOwnership::*-" + userId);
        Set<String> roleKeys = redisTemplate.keys("event-seating-ms::sessionRoleAccess::*-" + userId + "-*");

        if (!ownershipKeys.isEmpty()) {
            redisTemplate.delete(ownershipKeys);
            log.debug("Evicted ownership cache keys: {}", ownershipKeys);
        }

        if (!roleKeys.isEmpty()) {
            redisTemplate.delete(roleKeys);
            log.debug("Evicted role access cache keys: {}", roleKeys);
        }
    }
}
