package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.OrganizationOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Cacheable(value = "events", key = "#eventId")
    @Transactional(readOnly = true)
    public Event verifyOwnershipAndGetEvent(UUID eventId, String userId) {
        log.info("--- DATABASE HIT: Verifying event ownership and fetching event ID: {} ---", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        try {
            // Verify that the user owns the organization that owns this event
            organizationOwnershipService.verifyOwnershipAndGetOrganization(
                    event.getOrganization().getId(), userId);

            return event;
        } catch (AuthorizationDeniedException e) {
            throw new AuthorizationDeniedException("User does not have access to this event");
        }
    }
}
