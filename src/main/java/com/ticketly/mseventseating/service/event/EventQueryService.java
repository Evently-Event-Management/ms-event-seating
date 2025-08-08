package com.ticketly.mseventseating.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.*;
import com.ticketly.mseventseating.dto.event.SessionSeatingMapDTO;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.EventStatus;
import com.ticketly.mseventseating.model.Tier;
import com.ticketly.mseventseating.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventQueryService {

    private final EventRepository eventRepository;
    private final EventOwnershipService eventOwnershipService;
    private final ObjectMapper objectMapper;

    /**
     * Finds all events with optional status filtering
     *
     * @param status   Filter by status (optional)
     * @param pageable Pagination information
     * @return Page of event summaries
     */
    @Transactional(readOnly = true)
    public Page<EventSummaryDTO> findAllEvents(EventStatus status, Pageable pageable) {
        Page<Event> eventPage;

        if (status != null) {
            eventPage = eventRepository.findAllByStatus(status, pageable);
        } else {
            eventPage = eventRepository.findAll(pageable);
        }

        return eventPage.map(this::mapToEventSummary);
    }

    /**
     * Finds a specific event by ID with all its details
     * Checks if the user has permission to access this event.
     * Admin users bypass the organization ownership check.
     *
     * @param eventId Event ID
     * @param userId  The ID of the requesting user
     * @param isAdmin Whether the user has admin privileges
     * @return Detailed event information
     */
    @Transactional(readOnly = true)
    public EventDetailDTO findEventById(UUID eventId, String userId, boolean isAdmin) {
        Event event;

        if (isAdmin) {
            // Admin users can access any event
            event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
        } else {
            // Regular users need to be the organization owner
            try {
                event = eventOwnershipService.verifyOwnershipAndGetEvent(eventId, userId);
            } catch (AuthorizationDeniedException e) {
                throw new AuthorizationDeniedException("You don't have permission to access this event");
            }
        }

        return mapToEventDetail(event);
    }

    /**
     * Finds a specific event by ID with all its details (simplified method without authorization)
     * This should only be used when authorization is already handled at the controller level or for M2M queries.
     *
     * @param eventId Event ID
     * @return Detailed event information
     */
    @Transactional(readOnly = true)
    public EventDetailDTO findEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        return mapToEventDetail(event);
    }

    /**
     * Maps an Event entity to EventSummaryDTO for list view
     */
    private EventSummaryDTO mapToEventSummary(Event event) {
        // Find the earliest session
        OffsetDateTime earliestDate = event.getSessions().stream()
                .map(EventSession::getStartTime)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return EventSummaryDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .status(event.getStatus())
                .organizationName(event.getOrganization().getName())
                .organizationId(event.getOrganization().getId())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .description(event.getDescription() != null
                        ? (event.getDescription().length() > 150
                        ? event.getDescription().substring(0, 147) + "..."
                        : event.getDescription())
                        : null)
                .coverPhoto(event.getCoverPhotos() != null && !event.getCoverPhotos().isEmpty()
                        ? event.getCoverPhotos().getFirst()
                        : null)
                .sessionCount(event.getSessions().size())
                .earliestSessionDate(earliestDate)
                .build();
    }

    /**
     * Maps an Event entity to EventDetailDTO with all nested data
     */
    private EventDetailDTO mapToEventDetail(Event event) {
        List<TierDTO> tierDTOs = event.getTiers().stream()
                .map(this::mapToTierDTO)
                .collect(Collectors.toList());

        List<SessionDetailDTO> sessionDTOs = event.getSessions().stream()
                .map(this::mapToSessionDetailDTO)
                .collect(Collectors.toList());

        return EventDetailDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .overview(event.getOverview())
                .status(event.getStatus())
                .rejectionReason(event.getRejectionReason())
                .coverPhotos(event.getCoverPhotos())
                .organizationId(event.getOrganization().getId())
                .organizationName(event.getOrganization().getName())
                .categoryId(event.getCategory().getId())
                .categoryName(event.getCategory().getName())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .tiers(tierDTOs)
                .sessions(sessionDTOs)
                .build();
    }

    private TierDTO mapToTierDTO(Tier tier) {
        return TierDTO.builder()
                .id(tier.getId())
                .name(tier.getName())
                .color(tier.getColor())
                .price(tier.getPrice())
                .build();
    }

    private SessionDetailDTO mapToSessionDetailDTO(EventSession session) {
        SessionSeatingMapDTO layoutData = null;
        VenueDetailsDTO venueDetails = null;

        try {
            if (session.getSessionSeatingMap() != null && session.getSessionSeatingMap().getLayoutData() != null) {
                layoutData = objectMapper.readValue(
                        session.getSessionSeatingMap().getLayoutData(),
                        SessionSeatingMapDTO.class);
            }

            if (session.getVenueDetails() != null) {
                venueDetails = objectMapper.readValue(session.getVenueDetails(), VenueDetailsDTO.class);
            }
        } catch (IOException e) {
            log.error("Error parsing JSON data for session {}", session.getId(), e);
        }

        return SessionDetailDTO.builder()
                .id(session.getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .isOnline(session.isOnline())
                .onlineLink(session.getOnlineLink())
                .venueDetails(venueDetails)
                .salesStartRuleType(session.getSalesStartRuleType())
                .salesStartHoursBefore(session.getSalesStartHoursBefore())
                .salesStartFixedDatetime(session.getSalesStartFixedDatetime())
                .status(session.getStatus())
                .layoutData(layoutData)
                .build();
    }
}
