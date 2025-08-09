package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.OrganizationOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Service to verify ownership of events through their organizations.
 * Uses caching for better performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventOwnershipService {

    private final EventRepository eventRepository;
    private final OrganizationOwnershipService organizationOwnershipService;
    private final RedisTemplate<String, Object> redisTemplate;


    /**
     * Verifies that a user is the owner of an event's organization and returns the event.
     * The event is cached for better performance.
     *
     * @param eventId the event ID
     * @param userId  the user ID
     * @return the Event if the user is authorized
     * @throws ResourceNotFoundException    if the event doesn't exist
     * @throws AuthorizationDeniedException if the user is not the owner of the organization
     */
    @Cacheable(value = "events", key = "#eventId + '-' + #userId")
    @Transactional(readOnly = true)
    public Event verifyOwnershipAndGetEvent(UUID eventId, String userId) {
        log.info("Verifying event ownership for user: {} on event: {}", userId, eventId);
        log.debug("--- DATABASE HIT: Verifying event ownership and fetching event ID: {} ---", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Event not found with ID: {}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });

        log.debug("Found event: {} with title: '{}', organization: {}",
                eventId, event.getTitle(), event.getOrganization().getId());

        try {
            // Verify that the user owns the organization that owns this event
            log.debug("Verifying organization ownership for user: {} on organization: {}",
                    userId, event.getOrganization().getId());

            organizationOwnershipService.verifyOwnershipAndGetOrganization(
                    event.getOrganization().getId(), userId);

            log.info("User {} successfully verified as owner of event {}", userId, eventId);
            return event;
        } catch (AuthorizationDeniedException e) {
            log.warn("Authorization denied: User {} is not the owner of organization {} for event {}",
                    userId, event.getOrganization().getId(), eventId);
            throw new AuthorizationDeniedException("User does not have access to this event");
        }
    }

    /**
     * Evicts the event cache by event ID.
     *
     * @param eventId the event ID
     */
    public void evictEventCacheById(UUID eventId) {
        log.info("Evicting cache for event ID: {}", eventId);
        Set<String> keys = redisTemplate.keys("event-seating-ms::events::" + eventId + "-*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Evicted cache keys: {}", keys);
        } else {
            log.debug("No cache keys found for event ID: {}", eventId);
        }
    }
}
