package com.ticketly.mseventseating.service.session;

import com.ticketly.mseventseating.dto.session.OrganizationSessionDTO;
import com.ticketly.mseventseating.dto.session.SessionAnalyticsResponse;
import com.ticketly.mseventseating.dto.session.SessionStatusCountDTO;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.exception.UnauthorizedException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.repository.EventSessionRepository;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import com.ticketly.mseventseating.service.event.EventOwnershipService;
import com.ticketly.mseventseating.service.organization.OrganizationOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionAnalyticsService {

    private final EventSessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final EventOwnershipService eventOwnershipService;
    private final OrganizationOwnershipService organizationOwnershipService;

    /**
     * Get session analytics (counts by status) for a specific event
     * 
     * @param eventId The event ID
     * @param userId The user ID for ownership verification
     * @return SessionAnalyticsResponse with counts by status
     */
    @Transactional(readOnly = true)
    public SessionAnalyticsResponse getSessionAnalyticsForEvent(UUID eventId, String userId) {
        // Verify event exists and user has access
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        
        // Verify ownership unless user is admin
        boolean isAdmin = hasEventAdminRole();
        if (!isAdmin && !eventOwnershipService.isOwner(eventId, userId)) {
            throw new UnauthorizedException("User is not authorized to access this event's data");
        }
        
        // Get total count
        Long totalSessions = sessionRepository.countSessionsForEvent(eventId);
        
        // Get counts by status
        List<Object[]> countsByStatus = sessionRepository.countSessionsByStatusForEvent(eventId);
        List<SessionStatusCountDTO> statusCounts = convertToStatusCounts(countsByStatus);
        
        return SessionAnalyticsResponse.builder()
                .eventId(eventId)
                .eventTitle(event.getTitle())
                .organizationId(event.getOrganization().getId())
                .organizationName(event.getOrganization().getName())
                .totalSessions(totalSessions)
                .sessionsByStatus(statusCounts)
                .build();
    }
    
    /**
     * Get session analytics (counts by status) for a specific organization
     * 
     * @param organizationId The organization ID
     * @param userId The user ID for ownership verification
     * @return SessionAnalyticsResponse with counts by status
     */
    @Transactional(readOnly = true)
    public SessionAnalyticsResponse getSessionAnalyticsForOrganization(UUID organizationId, String userId) {
        // Verify organization exists and user has access
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));
        
        // Verify ownership unless user is admin
        boolean isAdmin = hasEventAdminRole();
        if (!isAdmin && !organizationOwnershipService.isOwner(organizationId, userId)) {
            throw new UnauthorizedException("User is not authorized to access this organization's data");
        }
        
        // Get total count
        Long totalSessions = sessionRepository.countSessionsForOrganization(organizationId);
        
        // Get counts by status
        List<Object[]> countsByStatus = sessionRepository.countSessionsByStatusForOrganization(organizationId);
        List<SessionStatusCountDTO> statusCounts = convertToStatusCounts(countsByStatus);
        
        return SessionAnalyticsResponse.builder()
                .organizationId(organizationId)
                .organizationName(organization.getName())
                .totalSessions(totalSessions)
                .sessionsByStatus(statusCounts)
                .build();
    }
    
    /**
     * Get all sessions for an organization with optional status filtering and sorting
     * 
     * @param organizationId The organization ID
     * @param userId The user ID for ownership verification
     * @param status Optional status filter
     * @param pageable Pagination and sorting information
     * @return Page of OrganizationSessionDTO with event details
     */
    @Transactional(readOnly = true)
    public Page<OrganizationSessionDTO> getOrganizationSessions(
            UUID organizationId, 
            String userId, 
            SessionStatus status, 
            Pageable pageable) {
        
        // Verify organization exists
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }
        
        // Verify ownership unless user is admin
        boolean isAdmin = hasEventAdminRole();
        if (!isAdmin && !organizationOwnershipService.isOwner(organizationId, userId)) {
            throw new UnauthorizedException("User is not authorized to access this organization's data");
        }
        
        // Get sessions with database level filtering, pagination, and sorting
        return sessionRepository.findSessionsByOrganization(organizationId, status, pageable);
    }
    
    /**
     * Convert raw query results to DTOs
     */
    private List<SessionStatusCountDTO> convertToStatusCounts(List<Object[]> countsByStatus) {
        List<SessionStatusCountDTO> result = new ArrayList<>();
        for (Object[] row : countsByStatus) {
            SessionStatus status = (SessionStatus) row[0];
            Long count = (Long) row[1];
            result.add(new SessionStatusCountDTO(status, count));
        }
        return result;
    }
    
    /**
     * Check if the current user has the event_admin role
     */
    private boolean hasEventAdminRole() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_event_admin"));
        }
        return false;
    }
}