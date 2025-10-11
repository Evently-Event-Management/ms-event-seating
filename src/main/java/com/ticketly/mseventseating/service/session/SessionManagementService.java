package com.ticketly.mseventseating.service.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.SessionRequest;
import com.ticketly.mseventseating.dto.session.CreateSessionsRequest;
import com.ticketly.mseventseating.dto.session.SessionBatchResponse;
import com.ticketly.mseventseating.dto.session.SessionLayoutUpdateDTO;
import com.ticketly.mseventseating.dto.session.SessionResponse;
import com.ticketly.mseventseating.dto.session.SessionStatusUpdateDTO;
import com.ticketly.mseventseating.dto.session.SessionTimeUpdateDTO;
import com.ticketly.mseventseating.dto.session.SessionVenueDetailsUpdateDTO;
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
import model.SessionType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> {
                    log.error("Event not found with ID: {}", request.getEventId());
                    return new ResourceNotFoundException("Event not found with ID: " + request.getEventId());
                });

        if (!eventOwnershipService.isOwner(event.getId(), userId)) {
            log.warn("User {} is not authorized to create sessions for event {}", userId, event.getId());
            throw new UnauthorizedException("User is not authorized to create sessions for this event");
        }

        validateSessionLimit(event, request.getSessions().size(), jwt);

        List<EventSession> createdSessions = new ArrayList<>();

        for (SessionRequest sessionDTO : request.getSessions()) {
            log.debug("Building session for startTime: {}, endTime: {}", sessionDTO.getStartTime(), sessionDTO.getEndTime());
            EventSession session = buildEventSession(sessionDTO, event);

            List<Tier> tiers = event.getTiers();

            String validatedLayoutData = prepareSessionLayout(sessionDTO.getLayoutData(), tiers);

            SessionSeatingMap map = SessionSeatingMap.builder()
                    .layoutData(validatedLayoutData)
                    .eventSession(session)
                    .build();

            session.setSessionSeatingMap(map);
            createdSessions.add(session);
        }

        List<EventSession> savedSessions = sessionRepository.saveAll(createdSessions);
        log.info("Successfully created {} sessions for event: {}", savedSessions.size(), event.getId());

        List<SessionResponse> sessionResponses = savedSessions.stream()
                .sorted(Comparator.comparing(EventSession::getStartTime))
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
                .orElseThrow(() -> {
                    log.error("Session not found with ID: {}", sessionId);
                    return new ResourceNotFoundException("Session not found with ID: " + sessionId);
                });

        if (!ownershipService.isOwner(sessionId, userId) &&
                !ownershipService.hasRole(sessionId, userId, OrganizationRole.SCANNER)) {
            log.warn("User {} is not authorized to access session {}", userId, sessionId);
            throw new UnauthorizedException("User is not authorized to access this session");
        }

        log.debug("User {} fetched session {}", userId, sessionId);
        return mapToSessionResponse(session);
    }


    /**
     * Get all sessions for an event
     */
    public List<SessionResponse> getSessionsByEvent(UUID eventId, String userId) {
        log.info("Fetching sessions for event ID: {}", eventId);

        eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event not found with ID: {}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });

        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} is not authorized to view sessions for event {}", userId, eventId);
            throw new UnauthorizedException("User is not authorized to view sessions for this event");
        }

        List<EventSession> sessions = sessionRepository.findAllByEventId(eventId);

        log.debug("Found {} sessions for event {}", sessions.size(), eventId);

        return sessions.stream()
                .sorted(Comparator.comparing(EventSession::getStartTime))
                .map(this::mapToSessionResponse)
                .toList();
    }

    /**
     * Update a session's time details
     */
    @Transactional
    public SessionResponse updateSessionTime(UUID sessionId, SessionTimeUpdateDTO updateDTO, String userId) {
        log.info("Updating session time details for ID: {}", sessionId);

        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.error("Session not found with ID: {}", sessionId);
                    return new ResourceNotFoundException("Session not found with ID: " + sessionId);
                });

        if (!ownershipService.isOwner(sessionId, userId)) {
            log.warn("User {} is not authorized to update session {}", userId, sessionId);
            throw new UnauthorizedException("User is not authorized to update this session");
        }

        try {
            validateSessionForTimeUpdate(session);
        } catch (BadRequestException e) {
            log.warn("Session time update validation failed for session {}: {}", sessionId, e.getMessage());
            throw e;
        }

        if (session.getStatus() == SessionStatus.ON_SALE &&
                !session.getSalesStartTime().isEqual(updateDTO.getSalesStartTime())) {
            log.warn("Attempt to change sales start time for ON_SALE session {}", sessionId);
            throw new BadRequestException("Cannot change sales start time once a session is ON_SALE.");
        }

        session.setStartTime(updateDTO.getStartTime());
        session.setEndTime(updateDTO.getEndTime());
        session.setSalesStartTime(updateDTO.getSalesStartTime());

        EventSession updatedSession = sessionRepository.save(session);

        ownershipService.evictSessionCacheById(sessionId);

        log.info("Successfully updated session time details: {}", updatedSession.getId());

        return mapToSessionResponse(updatedSession);
    }

    /**
     * Update a session's status
     * Business Logic:
     * - SCHEDULED -> ON_SALE or CANCELLED
     * - ON_SALE -> CLOSED only (one-way transition)
     * - SOLD_OUT: Cannot be manually set (system-determined)
     * - CLOSED: Final state
     * - CANCELLED: Final state
     */
    @Transactional
    public SessionResponse updateSessionStatus(UUID sessionId, SessionStatusUpdateDTO updateDTO, String userId) {
        log.info("Updating session status for ID: {} to {}", sessionId, updateDTO.getStatus());

        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.error("Session not found with ID: {}", sessionId);
                    return new ResourceNotFoundException("Session not found with ID: " + sessionId);
                });

        if (!ownershipService.isOwner(sessionId, userId)) {
            log.warn("User {} is not authorized to update status for session {}", userId, sessionId);
            throw new UnauthorizedException("User is not authorized to update this session's status");
        }

        try {
            validateStatusTransition(session, updateDTO.getStatus());
        } catch (BadRequestException e) {
            log.warn("Status transition validation failed for session {}: {}", sessionId, e.getMessage());
            throw e;
        }

        SessionStatus newStatus = updateDTO.getStatus();
        session.setStatus(newStatus);

        if (newStatus == SessionStatus.CANCELLED) {
            log.info("Session cancelled: {}. Additional cancellation logic would be applied here.", sessionId);
        }

        EventSession updatedSession = sessionRepository.save(session);

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

        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.error("Session not found with ID: {}", sessionId);
                    return new ResourceNotFoundException("Session not found with ID: " + sessionId);
                });

        if (!ownershipService.isOwner(sessionId, userId)) {
            log.warn("User {} is not authorized to update venue for session {}", userId, sessionId);
            throw new UnauthorizedException("User is not authorized to update this session's venue");
        }

        try {
            validateSessionForVenueUpdate(session);
        } catch (BadRequestException e) {
            log.warn("Venue update validation failed for session {}: {}", sessionId, e.getMessage());
            throw e;
        }

        session.setSessionType(updateDTO.getSessionType());

        try {
            session.setVenueDetails(objectMapper.writeValueAsString(updateDTO.getVenueDetails()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize venue details for session {}", sessionId, e);
            throw new BadRequestException("Invalid venue details format.");
        }

        List<Tier> tiers = session.getEvent().getTiers();

        String validatedLayoutData = prepareSessionLayout(updateDTO.getLayoutData(), tiers);

        SessionSeatingMap seatingMap = session.getSessionSeatingMap();
        seatingMap.setLayoutData(validatedLayoutData);

        EventSession updatedSession = sessionRepository.save(session);

        ownershipService.evictSessionCacheById(sessionId);

        log.info("Successfully updated session venue and seating map: {}", updatedSession.getId());

        return mapToSessionResponse(updatedSession);
    }

    /**
     * Update only a session's venue details
     */
    @Transactional
    public SessionResponse updateSessionVenueDetails(UUID sessionId, SessionVenueDetailsUpdateDTO updateDTO, String userId) {
        log.info("Updating venue details for session ID: {}", sessionId);

        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.error("Session not found with ID: {}", sessionId);
                    return new ResourceNotFoundException("Session not found with ID: " + sessionId);
                });

        if (!ownershipService.isOwner(sessionId, userId)) {
            log.warn("User {} is not authorized to update venue details for session {}", userId, sessionId);
            throw new UnauthorizedException("User is not authorized to update this session's venue details");
        }

        try {
            validateSessionForVenueUpdate(session);
            validateVenueDetailsForSessionType(session.getSessionType(), updateDTO.getVenueDetails());
        } catch (BadRequestException e) {
            log.warn("Venue details update validation failed for session {}: {}", sessionId, e.getMessage());
            throw e;
        }

        try {
            session.setVenueDetails(objectMapper.writeValueAsString(updateDTO.getVenueDetails()));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize venue details for session {}", sessionId, e);
            throw new BadRequestException("Invalid venue details format.");
        }

        EventSession updatedSession = sessionRepository.save(session);

        ownershipService.evictSessionCacheById(sessionId);

        log.info("Successfully updated session venue details: {}", updatedSession.getId());

        return mapToSessionResponse(updatedSession);
    }

    /**
     * Update only a session's seating layout
     */
    @Transactional
    public SessionResponse updateSessionLayout(UUID sessionId, SessionLayoutUpdateDTO updateDTO, String userId) {
        log.info("Updating seating layout for session ID: {}", sessionId);

        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.error("Session not found with ID: {}", sessionId);
                    return new ResourceNotFoundException("Session not found with ID: " + sessionId);
                });

        if (!ownershipService.isOwner(sessionId, userId)) {
            log.warn("User {} is not authorized to update layout for session {}", userId, sessionId);
            throw new UnauthorizedException("User is not authorized to update this session's layout");
        }

        try {
            validateSessionForVenueUpdate(session);
        } catch (BadRequestException e) {
            log.warn("Seating layout update validation failed for session {}: {}", sessionId, e.getMessage());
            throw e;
        }

        List<Tier> tiers = session.getEvent().getTiers();

        String validatedLayoutData = prepareSessionLayout(updateDTO.getLayoutData(), tiers);

        SessionSeatingMap seatingMap = session.getSessionSeatingMap();
        seatingMap.setLayoutData(validatedLayoutData);

        EventSession updatedSession = sessionRepository.save(session);

        ownershipService.evictSessionCacheById(sessionId);

        log.info("Successfully updated session seating layout: {}", updatedSession.getId());

        return mapToSessionResponse(updatedSession);
    }

    /**
     * Delete a session
     */
    @Transactional
    public void deleteSession(UUID sessionId, String userId) {
        log.info("Deleting session with ID: {}", sessionId);

        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.error("Session not found with ID: {}", sessionId);
                    return new ResourceNotFoundException("Session not found with ID: " + sessionId);
                });

        if (!ownershipService.isOwner(sessionId, userId)) {
            log.warn("User {} is not authorized to delete session {}", userId, sessionId);
            throw new UnauthorizedException("User is not authorized to delete this session");
        }

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            log.warn("Attempt to delete session {} with status {}", sessionId, session.getStatus());
            throw new BadRequestException("Only SCHEDULED sessions can be deleted. Current status: " + session.getStatus());
        }

        sessionRepository.delete(session);

        ownershipService.evictSessionCacheById(sessionId);

        log.info("Successfully deleted session: {}", sessionId);
    }

    /**
     * Map EventSession to SessionResponse
     */
    private SessionResponse mapToSessionResponse(EventSession session) {
        VenueDetailsDTO venueDetails = parseVenueDetails(session.getVenueDetails());

        // Parse layout data from session seating map
        SessionSeatingMapDTO layoutData = null;
        try {
            if (session.getSessionSeatingMap() != null && session.getSessionSeatingMap().getLayoutData() != null) {
                layoutData = objectMapper.readValue(
                    session.getSessionSeatingMap().getLayoutData(),
                    SessionSeatingMapDTO.class);
            }
        } catch (IOException e) {
            log.error("Error parsing layout data for session {}", session.getId(), e);
        }

        return SessionResponse.builder()
                .id(session.getId())
                .eventId(session.getEvent().getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .salesStartTime(session.getSalesStartTime())
                .sessionType(session.getSessionType())
                .status(session.getStatus())
                .venueDetails(venueDetails)
                .layoutData(layoutData)
                .build();
    }

    /**
     * Validate that adding more sessions won't exceed the subscription limit
     */
    private void validateSessionLimit(Event event, int additionalSessions, Jwt jwt) {
        int maxSessions = limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        int currentSessionCount = event.getSessions().size();

        log.debug("Validating session limit: current={}, additional={}, max={}", currentSessionCount, additionalSessions, maxSessions);

        if (currentSessionCount + additionalSessions > maxSessions) {
            log.warn("Session limit exceeded for event {}: attempted={}, max={}", event.getId(), currentSessionCount + additionalSessions, maxSessions);
            throw new BadRequestException("Cannot add " + additionalSessions +
                    " sessions. You can only have a maximum of " + maxSessions +
                    " sessions per event for your subscription tier.");
        }
    }

    /**
     * Build EventSession from SessionRequest
     */
    private EventSession buildEventSession(SessionRequest dto, Event event) {
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
     * Only SCHEDULED and ON_SALE sessions can have their times updated
     */
    private void validateSessionForTimeUpdate(EventSession session) {
        SessionStatus status = session.getStatus();
        if (status == SessionStatus.CLOSED || status == SessionStatus.CANCELLED || status == SessionStatus.SOLD_OUT) {
            log.warn("Cannot update times for session {} with status {}", session.getId(), status);
            throw new BadRequestException("Cannot update times for a session with status: " + status);
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (session.getStartTime().isBefore(now)) {
            log.warn("Cannot update session {} that has already started", session.getId());
            throw new BadRequestException("Cannot update a session that has already started");
        }
    }

    /**
     * Validate if the session status transition is allowed
     * Valid transitions:
     * - SCHEDULED -> ON_SALE or CANCELLED
     * - ON_SALE -> CLOSED only (one-way transition, cannot go back to SCHEDULED)
     * - SOLD_OUT (can't be manually set)
     * - CLOSED (final state)
     * - CANCELLED (final state)
     */
    private void validateStatusTransition(EventSession session, SessionStatus newStatus) {
        SessionStatus currentStatus = session.getStatus();

        log.debug("Validating status transition for session {}: {} -> {}", session.getId(), currentStatus, newStatus);

        if (currentStatus == newStatus) {
            log.debug("No status change for session {}", session.getId());
            return;
        }
        
        if (newStatus == SessionStatus.SOLD_OUT) {
            log.warn("Attempt to manually set SOLD_OUT status for session {}", session.getId());
            throw new BadRequestException("Status SOLD_OUT is determined by the system and cannot be manually set.");
        }

        switch (currentStatus) {
            case SCHEDULED:
                if (newStatus != SessionStatus.ON_SALE && newStatus != SessionStatus.CANCELLED) {
                    log.warn("Invalid status transition from SCHEDULED to {} for session {}", newStatus, session.getId());
                    throw new BadRequestException("From SCHEDULED, you can only change to ON_SALE or CANCELLED.");
                }
                break;
            case ON_SALE:
                if (newStatus != SessionStatus.CLOSED) {
                    log.warn("Invalid status transition from ON_SALE to {} for session {}", newStatus, session.getId());
                    throw new BadRequestException("From ON_SALE, you can only change to CLOSED. Cannot revert back to SCHEDULED once sales have started.");
                }
                break;
            case SOLD_OUT:
                log.warn("Attempt to change status of SOLD_OUT session {}", session.getId());
                throw new BadRequestException("Cannot manually change status of a SOLD_OUT session.");
            case CLOSED:
                log.warn("Attempt to change status of CLOSED session {}", session.getId());
                throw new BadRequestException("Cannot change status of a CLOSED session; it is a final state.");
            case CANCELLED:
                log.warn("Attempt to change status of CANCELLED session {}", session.getId());
                throw new BadRequestException("Cannot change status of a CANCELLED session; it is a final state.");
            default:
                log.error("Unknown session status {} for session {}", currentStatus, session.getId());
                throw new BadRequestException("Unknown session status: " + currentStatus);
        }
    }

    /**
     * Validate if the session is in a state that allows venue updates
     * Only SCHEDULED sessions can have their venue details updated
     */
    private void validateSessionForVenueUpdate(EventSession session) {
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            log.warn("Cannot update venue details for session {} in state {}", session.getId(), session.getStatus());
            throw new BadRequestException("Can only update venue details for sessions in SCHEDULED state. Current state: " + session.getStatus());
        }
    }
    
    /**
     * Validates that the venue details are appropriate for the session type
     * For online sessions, online link must be provided
     * For physical sessions, venue name must be provided
     */
    private void validateVenueDetailsForSessionType(SessionType sessionType, VenueDetailsDTO venueDetails) {
        if (sessionType == SessionType.ONLINE) {
            // For ONLINE sessions, the onlineLink must be present and not blank
            if (venueDetails == null || venueDetails.getOnlineLink() == null || venueDetails.getOnlineLink().isBlank()) {
                log.warn("Online link is required for online sessions");
                throw new BadRequestException("An online link is required for online sessions.");
            }
        } else if (sessionType == SessionType.PHYSICAL) {
            // For PHYSICAL sessions, the venue name must be present and not blank
            if (venueDetails == null || venueDetails.getName() == null || venueDetails.getName().isBlank()) {
                log.warn("Venue name is required for physical sessions");
                throw new BadRequestException("A venue name is required for physical sessions.");
            }
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
     * @param tiers      The list of tiers to validate against
     * @return Validated layout data as a JSON string
     */
    private String prepareSessionLayout(SessionSeatingMapDTO layoutData, List<Tier> tiers) {
        try {
            if (layoutData == null || layoutData.getLayout() == null || layoutData.getLayout().getBlocks() == null) {
                log.warn("Layout data or blocks cannot be null.");
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
                boolean tierExists = tiers.stream()
                        .anyMatch(tier -> tier.getId().equals(seat.getTierId()));
                if (!tierExists) {
                    log.warn("Seat/slot assigned to invalid Tier ID: {}", seat.getTierId());
                    throw new BadRequestException("Seat/slot is assigned to an invalid Tier ID: " + seat.getTierId());
                }
            } else {
                log.warn("Seat missing Tier ID");
                throw new BadRequestException("Seat must be assigned to a valid Tier ID.");
            }
        }
    }


}

