package com.ticketly.mseventseating.security;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.OrganizationOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSecurityService {

    private final EventRepository eventRepository;
    private final OrganizationOwnershipService organizationOwnershipService;

    /**
     * Checks if the authenticated user is the owner of the organization that owns the event.
     * Used for PreAuthorize annotations in controllers.
     * 
     * @param eventId the event ID to check
     * @param authentication the current authentication object
     * @return true if the user is the owner of the event's organization, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isEventOrganizationOwner(UUID eventId, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        
        // Extract the user ID from JWT
        String userId;
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            userId = jwt.getSubject();
        } else {
            // For other authentication types, try to get principal
            userId = authentication.getName();
        }
        
        try {
            // Find the event
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
            
            UUID organizationId = event.getOrganization().getId();
            
            // Check if the user is the owner of the organization
            // This will use the cached implementation in OrganizationOwnershipService
            try {
                organizationOwnershipService.verifyOwnershipAndGetOrganization(organizationId, userId);
                return true;
            } catch (Exception e) {
                log.debug("User {} is not the owner of organization {}", userId, organizationId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error checking event organization ownership", e);
            return false;
        }
    }
}
