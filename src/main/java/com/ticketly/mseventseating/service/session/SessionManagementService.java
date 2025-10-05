package com.ticketly.mseventseating.service.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.session.CreateSessionsRequest;
import com.ticketly.mseventseating.dto.session.SessionBatchResponse;
import com.ticketly.mseventseating.dto.session.SessionCreationDTO;
import com.ticketly.mseventseating.dto.session.SessionResponse;
import com.ticketly.mseventseating.dto.session.SessionTimeUpdateDTO;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.exception.UnauthorizedException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.OrganizationRole;
import com.ticketly.mseventseating.model.SessionSeatingMap;
import com.ticketly.mseventseating.model.SubscriptionLimitType;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.repository.EventSessionRepository;
import com.ticketly.mseventseating.service.event.EventOwnershipService;
import com.ticketly.mseventseating.service.limts.LimitService;
import com.ticketly.mseventseating.dto.event.VenueDetailsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.SessionStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final EventSessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final LimitService limitService;
    private final SessionOwnershipService ownershipService;
    private final EventOwnershipService eventOwnershipService;

    /**
     * Create multiple sessions for an event
     */
    @Transactional
    public SessionBatchResponse createSessions(CreateSessionsRequest request, String userId, Jwt jwt) {
        log.info("Creating {} sessions for event: {}", request.getSessions().size(), request.getEventId());
        
        // 1. Find the event
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + request.getEventId()));
        
        // 2. Validate ownership - only event owners can create sessions
        if (!eventOwnershipService.isOwner(event.getId(), userId)) {
            throw new UnauthorizedException("User is not authorized to create sessions for this event");
        }
        
        // 3. Validate session limit
        validateSessionLimit(event, request.getSessions().size(), jwt);
        
        // 4. Create and save sessions
        List<EventSession> createdSessions = new ArrayList<>();
        
        for (SessionCreationDTO sessionDTO : request.getSessions()) {
            EventSession session = buildEventSession(sessionDTO, event);
            
            // Create the seating map
            SessionSeatingMap map = SessionSeatingMap.builder()
                    .layoutData(convertToJsonString(sessionDTO.getLayoutData()))
                    .eventSession(session)
                    .build();
                    
            session.setSessionSeatingMap(map);
            createdSessions.add(session);
        }
        
        List<EventSession> savedSessions = sessionRepository.saveAll(createdSessions);
        log.info("Successfully created {} sessions for event: {}", savedSessions.size(), event.getId());
        
        // 5. Map to response
        List<SessionResponse> sessionResponses = savedSessions.stream()
                .map(this::mapToSessionResponse)
                .toList();
                
        return SessionBatchResponse.builder()
                .eventId(event.getId())
                .totalCreated(sessionResponses.size())
                .sessions(sessionResponses)
                .build();
    }
    
    /**
     * Get a session by ID
     */
    public SessionResponse getSession(UUID sessionId, String userId) {
        log.info("Fetching session with ID: {}", sessionId);
        
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));
        
        // Use ownership service to check if user has access
        if (!ownershipService.isOwner(sessionId, userId) && 
            !ownershipService.hasRole(sessionId, userId, OrganizationRole.SCANNER)) {
            throw new UnauthorizedException("User is not authorized to access this session");
        }
        
        return mapToSessionResponse(session);
    }
    
    /**
     * Update a session
     */
    @Transactional
    public SessionResponse updateSession(UUID sessionId, SessionTimeUpdateDTO updateDTO, String userId) {
        log.info("Updating session with ID: {}", sessionId);
        
        // 1. Find the session
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));
        
        // 2. Validate ownership - Only owners can update sessions, not scanners
        if (!ownershipService.isOwner(sessionId, userId)) {
            throw new UnauthorizedException("User is not authorized to update this session");
        }
        
        // 3. Check if session is active or past
        validateSessionIsEditable(session);
        
        // 4. Update session fields
        session.setStartTime(updateDTO.getStartTime());
        session.setEndTime(updateDTO.getEndTime());
        session.setSalesStartTime(updateDTO.getSalesStartTime());
        session.setSessionType(updateDTO.getSessionType());
        
        try {
            session.setVenueDetails(objectMapper.writeValueAsString(updateDTO.getVenueDetails()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize venue details for session", e);
            throw new BadRequestException("Invalid venue details format.");
        }
        
        EventSession updatedSession = sessionRepository.save(session);
        
        // 5. Invalidate cache for this session
        ownershipService.evictSessionCacheById(sessionId);
        
        log.info("Successfully updated session: {}", updatedSession.getId());
        
        return mapToSessionResponse(updatedSession);
    }
    
    /**
     * Delete a session
     */
    @Transactional
    public void deleteSession(UUID sessionId, String userId) {
        log.info("Deleting session with ID: {}", sessionId);
        
        // 1. Find the session
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));
        
        // 2. Validate ownership - Only owners can delete sessions
        if (!ownershipService.isOwner(sessionId, userId)) {
            throw new UnauthorizedException("User is not authorized to delete this session");
        }
        
        // 3. Check if session is active or past
        validateSessionIsEditable(session);
        
        // 4. Delete the session
        sessionRepository.delete(session);
        
        // 5. Invalidate cache for this session
        ownershipService.evictSessionCacheById(sessionId);
        
        log.info("Successfully deleted session: {}", sessionId);
    }
    
    /**
     * Map EventSession to SessionResponse
     */
    private SessionResponse mapToSessionResponse(EventSession session) {
        VenueDetailsDTO venueDetails = parseVenueDetails(session.getVenueDetails());
        
        return SessionResponse.builder()
                .id(session.getId())
                .eventId(session.getEvent().getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .salesStartTime(session.getSalesStartTime())
                .sessionType(session.getSessionType())
                .status(session.getStatus())
                .venueDetails(venueDetails)
                .build();
    }
    
    /**
     * Validate that the session is editable (not active or past)
     */
    private void validateSessionIsEditable(EventSession session) {
        // If sales have started or the session has ended, it's not editable
        if (OffsetDateTime.now().isAfter(session.getSalesStartTime()) || 
                OffsetDateTime.now().isAfter(session.getEndTime())) {
            throw new BadRequestException("Cannot edit a session after sales have started or the session has ended");
        }
    }
    
    /**
     * Validate that adding more sessions won't exceed the subscription limit
     */
    private void validateSessionLimit(Event event, int additionalSessions, Jwt jwt) {
        int maxSessions = limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        int currentSessionCount = event.getSessions().size();
        
        if (currentSessionCount + additionalSessions > maxSessions) {
            throw new BadRequestException("Cannot add " + additionalSessions + 
                    " sessions. You can only have a maximum of " + maxSessions + 
                    " sessions per event for your subscription tier.");
        }
    }
    
    /**
     * Build EventSession from SessionCreationDTO
     */
    private EventSession buildEventSession(SessionCreationDTO dto, Event event) {
        String venueDetailsJson;
        try {
            venueDetailsJson = objectMapper.writeValueAsString(dto.getVenueDetails());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize venue details for session", e);
            throw new BadRequestException("Invalid venue details format.");
        }
        
        return EventSession.builder()
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .event(event)
                .salesStartTime(dto.getSalesStartTime())
                .sessionType(dto.getSessionType())
                .venueDetails(venueDetailsJson)
                .status(SessionStatus.SCHEDULED)
                .build();
    }
    
    /**
     * Parse venue details from JSON string
     */
    private VenueDetailsDTO parseVenueDetails(String venueDetailsJson) {
        if (venueDetailsJson == null) {
            return new VenueDetailsDTO();
        }
        
        try {
            return objectMapper.readValue(venueDetailsJson, VenueDetailsDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse venue details", e);
            return new VenueDetailsDTO();
        }
    }
    
    /**
     * Convert layout data to JSON string
     */
    private String convertToJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            throw new BadRequestException("Invalid JSON format");
        }
    }
}