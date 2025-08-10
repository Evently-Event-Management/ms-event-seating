package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.SeatingLayoutTemplate;
import com.ticketly.mseventseating.repository.SeatingLayoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Service to verify ownership of seating layout templates through their organizations.
 * Uses caching for better performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatingLayoutTemplateOwnershipService {

    private final SeatingLayoutTemplateRepository seatingLayoutTemplateRepository;
    private final OrganizationOwnershipService organizationOwnershipService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Verifies that a user is the owner of a template's organization.
     * The boolean result of this check is cached.
     *
     * @param templateId the template ID
     * @param userId     the user ID
     * @return true if the user is the owner
     * @throws ResourceNotFoundException if the template doesn't exist
     */
    @Cacheable(value = "templateOwnership", key = "#templateId + '-' + #userId")
    @Transactional(readOnly = true)
    public boolean isOwner(UUID templateId, String userId) {
        log.info("--- DATABASE HIT: Verifying template ownership for template ID: {} ---", templateId);

        SeatingLayoutTemplate template = seatingLayoutTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Seating layout template not found with ID: " + templateId));

        // Delegate to organizationOwnershipService to check ownership
        return organizationOwnershipService.isOwner(template.getOrganization().getId(), userId);
    }

    /**
     * Evicts the template cache by template ID.
     *
     * @param templateId the template ID
     */
    public void evictTemplateCacheById(UUID templateId) {
        Set<String> keys = redisTemplate.keys("event-seating-ms::templateOwnership::" + templateId + "-*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Evicted template cache for ID: {}", templateId);
        } else {
            log.debug("No cache entries found for template ID: {}", templateId);
        }
    }
}
