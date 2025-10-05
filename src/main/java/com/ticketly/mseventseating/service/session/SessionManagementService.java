package com.ticketly.mseventseating.service.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.session.CreateSessionsRequest;
import com.ticketly.mseventseating.dto.session.SessionBatchResponse;
import com.ticketly.mseventseating.dto.session.SessionCreationDTO;
import com.ticketly.mseventseating.dto.session.SessionResponse;
import com.ticketly.mseventseating.dto.session.SessionStatusUpdateDTO;
import com.ticketly.mseventseating.dto.session.SessionTimeUpdateDTO;
import com.ticketly.mseventseating.dto.session.SessionVenueUpdateDTO;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.exception.UnauthorizedException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.repository.EventSessionRepository;
import com.ticketly.mseventseating.service.event.EventOwnershipService;
import com.ticketly.mseventseating.service.limts.LimitService;
import com.ticketly.mseventseating.dto.event.VenueDetailsDTO;
import dto.SessionSeatingMapDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.SeatStatus;
import model.SessionStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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

            // Get tiers from the event
            List<Tier> tiers = event.getTiers();
            
            // Validate and prepare layout data
            String validatedLayoutData = prepareSessionLayout(sessionDTO.getLayoutData(), tiers);
            
            // Create the seating map
            SessionSeatingMap map = SessionSeatingMap.builder()
                    .layoutData(validatedLayoutData)
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
     * Update a session's time details
     */
    @Transactional
    public SessionResponse updateSessionTime(UUID sessionId, SessionTimeUpdateDTO updateDTO, String userId) {
        log.info("Updating session time details for ID: {}", sessionId);

        // 1. Find the session
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        // 2. Validate ownership - Only owners can update sessions, not scanners
        if (!ownershipService.isOwner(sessionId, userId)) {
            throw new UnauthorizedException("User is not authorized to update this session");
        }

        // 3. Check if session status allows time update
        validateSessionForTimeUpdate(session);

        //If session is ON_SALE and sales start time changes ensure session put back into SCHEDULED
        if (session.getStatus() == SessionStatus.ON_SALE &&
                !session.getSalesStartTime().isEqual(updateDTO.getSalesStartTime())) {
            session.setStatus(SessionStatus.SCHEDULED);
        }

        // 4. Update time fields
        session.setStartTime(updateDTO.getStartTime());
        session.setEndTime(updateDTO.getEndTime());
        session.setSalesStartTime(updateDTO.getSalesStartTime());

        // 5. Save changes
        EventSession updatedSession = sessionRepository.save(session);

        // 6. Invalidate cache for this session
        ownershipService.evictSessionCacheById(sessionId);

        log.info("Successfully updated session time details: {}", updatedSession.getId());

        return mapToSessionResponse(updatedSession);
    }

    /**
     * Update a session's status
     */
    @Transactional
    public SessionResponse updateSessionStatus(UUID sessionId, SessionStatusUpdateDTO updateDTO, String userId) {
        log.info("Updating session status for ID: {}", sessionId);

        // 1. Find the session
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        // 2. Validate ownership - Only owners can update status, not scanners
        if (!ownershipService.isOwner(sessionId, userId)) {
            throw new UnauthorizedException("User is not authorized to update this session's status");
        }

        // 3. Validate status transition
        validateStatusTransition(session, updateDTO.getStatus());

        // 4. Update status
        session.setStatus(updateDTO.getStatus());

        // 5. Save changes
        EventSession updatedSession = sessionRepository.save(session);

        // 6. Invalidate cache for this session
        ownershipService.evictSessionCacheById(sessionId);

        log.info("Successfully updated session status to {}: {}", updateDTO.getStatus(), updatedSession.getId());

        return mapToSessionResponse(updatedSession);
    }

    /**
     * Update a session's venue details and seating map
     */
    @Transactional
    public SessionResponse updateSessionVenue(UUID sessionId, SessionVenueUpdateDTO updateDTO, String userId) {
        log.info("Updating venue and seating map for session ID: {}", sessionId);

        // 1. Find the session
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID: " + sessionId));

        // 2. Validate ownership - Only owners can update venue, not scanners
        if (!ownershipService.isOwner(sessionId, userId)) {
            throw new UnauthorizedException("User is not authorized to update this session's venue");
        }

        // 3. Validate session is in SCHEDULED state for venue update
        validateSessionForVenueUpdate(session);

        // 4. Update session fields
        session.setSessionType(updateDTO.getSessionType());

        try {
            session.setVenueDetails(objectMapper.writeValueAsString(updateDTO.getVenueDetails()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize venue details for session", e);
            throw new BadRequestException("Invalid venue details format.");
        }

        // 5. Get tiers from the event
        List<Tier> tiers = session.getEvent().getTiers();
        
        // 6. Validate and prepare layout data
        String validatedLayoutData = prepareSessionLayout(updateDTO.getLayoutData(), tiers);
        
        // 7. Update seating map
        SessionSeatingMap seatingMap = session.getSessionSeatingMap();
        seatingMap.setLayoutData(validatedLayoutData);

        // 8. Save changes
        EventSession updatedSession = sessionRepository.save(session);

        // 9. Invalidate cache for this session
        ownershipService.evictSessionCacheById(sessionId);

        log.info("Successfully updated session venue and seating map: {}", updatedSession.getId());

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

        //Can delete only SCHEDULED sessions before sales start
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BadRequestException("Only SCHEDULED sessions can be deleted");
        }

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
     * Utility method to convert any object to a JSON string
     * Not currently used as we have more specific methods for layout data.
     */
    @SuppressWarnings("unused") 
    private String convertToJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            throw new BadRequestException("Invalid JSON format");
        }
    }

    /**
     * Validate if the session is in a state that allows time updates
     * SCHEDULED and ON_SALE sessions can have their times updated
     */
    private void validateSessionForTimeUpdate(EventSession session) {
        if (session.getStatus() == SessionStatus.CLOSED) {
            throw new BadRequestException("Cannot update times for a CLOSED session");
        }

        // Additional validation logic for time updates
        OffsetDateTime now = OffsetDateTime.now();
        if (session.getStartTime().isBefore(now)) {
            throw new BadRequestException("Cannot update a session that has already started");
        }
    }

    /**
     * Validate if the session status transition is allowed
     * Valid transitions: SCHEDULED -> ON_SALE -> CLOSED
     */
    private void validateStatusTransition(EventSession session, SessionStatus newStatus) {
        SessionStatus currentStatus = session.getStatus();

        // No change in status is always allowed
        if (currentStatus == newStatus) {
            return;
        }

        switch (currentStatus) {
            case SCHEDULED:
                // From SCHEDULED, can only go to ON_SALE
                if (newStatus != SessionStatus.ON_SALE) {
                    throw new BadRequestException("Cannot transition from SCHEDULED to " + newStatus + ". Must go to ON_SALE first.");
                }
                break;
            case ON_SALE:
                if (newStatus != SessionStatus.CLOSED) {
                    throw new BadRequestException("Cannot transition from ON_SALE to " + newStatus + ". Can only transition to CLOSED.");
                }
                break;
            case CLOSED:
                throw new BadRequestException("Cannot change status of a CLOSED session");
            default:
                throw new BadRequestException("Unknown session status: " + currentStatus);
        }
    }

    /**
     * Validate if the session is in a state that allows venue updates
     * Only SCHEDULED sessions can have their venue details updated
     */
    private void validateSessionForVenueUpdate(EventSession session) {
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BadRequestException("Can only update venue details for sessions in SCHEDULED state");
        }
    }


    /**
     * Prepares and validates session layout data.
     * This includes:
     * 1. Ensuring all blocks, rows, and seats have valid UUIDs
     * 2. Validating that seats have valid tier assignments
     * 3. Setting the correct seat status
     * 
     * @param layoutData The layout data to validate
     * @param tiers The list of tiers to validate against
     * @return Validated layout data as a JSON string
     */
    private String prepareSessionLayout(SessionSeatingMapDTO layoutData, List<Tier> tiers) {
        try {
            if (layoutData == null || layoutData.getLayout() == null || layoutData.getLayout().getBlocks() == null) {
                throw new BadRequestException("Layout data or blocks cannot be null.");
            }

            for (SessionSeatingMapDTO.Block block : layoutData.getLayout().getBlocks()) {
                block.setId(UUID.randomUUID());

                if ("seated_grid".equals(block.getType())) {
                    if (block.getRows() == null) continue;
                    for (SessionSeatingMapDTO.Row row : block.getRows()) {
                        row.setId(UUID.randomUUID());
                        if (row.getSeats() != null) {
                            validateSeats(row.getSeats(), tiers);
                        }
                    }
                } else if ("standing_capacity".equals(block.getType())) {
                    if (block.getSeats() != null) {
                        validateSeats(block.getSeats(), tiers);
                    }
                }
            }
            return objectMapper.writeValueAsString(layoutData);
        } catch (IOException e) {
            log.error("Invalid session layout data", e);
            throw new BadRequestException("Invalid session layout data: " + e.getMessage());
        }
    }


    private void validateSeats(List<SessionSeatingMapDTO.Seat> seats, List<Tier> tiers) {
        for (SessionSeatingMapDTO.Seat seat : seats) {
            seat.setId(UUID.randomUUID());
            if (seat.getStatus() == SeatStatus.RESERVED) {
                continue;
            }

            seat.setStatus(SeatStatus.AVAILABLE);

            if (seat.getTierId() != null) {
                // Check if the tierId exists in the provided tiers list
                boolean tierExists = tiers.stream()
                        .anyMatch(tier -> tier.getId().equals(seat.getTierId()));
                if (!tierExists) {
                    throw new BadRequestException("Seat/slot is assigned to an invalid Tier ID: " + seat.getTierId());
                }
            } else {
                throw new BadRequestException("Seat must be assigned to a valid Tier ID.");
            }
        }
    }


}