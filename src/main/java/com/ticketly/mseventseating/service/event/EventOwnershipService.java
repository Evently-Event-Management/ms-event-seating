package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;


    /**
     * Verifies that a user is the owner of an event's organization.
     * The boolean result of this check is cached.
     *
     * @param eventId the event ID
     * @param userId  the user ID
     * @return true if the user is the owner
     * @throws ResourceNotFoundException if the event doesn't exist
     */
    @Cacheable(value = "eventOwnership", key = "#eventId + '-' + #userId")
    @Transactional(readOnly = true)
    public boolean isOwner(UUID eventId, String userId) {
        log.info("--- DATABASE HIT: Verifying event ownership for event ID: {} ---", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        // Direct check on the organization's userId
        return event.getOrganization().getUserId().equals(userId);
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
