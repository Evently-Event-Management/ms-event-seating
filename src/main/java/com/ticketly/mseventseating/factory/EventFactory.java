package com.ticketly.mseventseating.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.LayoutDataDTO;
import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.SessionRequest;
import com.ticketly.mseventseating.dto.event.TierRequest;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.CategoryRepository;
import com.ticketly.mseventseating.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventFactory {

    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    // Removed seatingLayoutTemplateRepository dependency
    private final ObjectMapper objectMapper;

    public Event createFromRequest(CreateEventRequest request, Organization organization) {
        Venue venue = findVenue(request.getVenueId());
        Set<Category> categories = findCategories(request.getCategoryIds());

        Event event = buildEventEntity(request, organization, venue, categories);

        List<Tier> tiers = buildTiers(request.getTiers(), event);
        event.setTiers(tiers);

        // Use the sessionLayoutData directly from request instead of fetching a template
        List<EventSession> sessions = buildSessions(request.getSessions(), event, request.getSessionLayoutData());
        event.setSessions(sessions);

        return event;
    }

    private Event buildEventEntity(CreateEventRequest request, Organization org, Venue venue, Set<Category> categories) {
        return Event.builder()
                .title(request.getTitle()).description(request.getDescription()).overview(request.getOverview())
                .coverPhotos(request.getCoverPhotos()).organization(org).venue(venue).categories(categories)
                .isOnline(request.isOnline()).onlineLink(request.getOnlineLink())
                .locationDescription(request.getLocationDescription())
                .build();
    }

    private List<Tier> buildTiers(List<TierRequest> tierRequests, Event event) {
        return tierRequests.stream()
                .map(req -> Tier.builder().name(req.getName()).price(req.getPrice()).color(req.getColor()).event(event).build())
                .collect(Collectors.toList());
    }

    // Updated to accept sessionLayoutData string directly instead of a template entity
    private List<EventSession> buildSessions(List<SessionRequest> sessionRequests, Event event, String sessionLayoutData) {
        List<EventSession> sessions = new ArrayList<>();
        for (SessionRequest req : sessionRequests) {
            EventSession session = EventSession.builder()
                    .startTime(req.getStartTime())
                    .endTime(req.getEndTime())
                    .event(event)
                    .salesStartRuleType(req.getSalesStartRuleType())
                    .salesStartHoursBefore(req.getSalesStartHoursBefore())
                    .salesStartFixedDatetime(req.getSalesStartFixedDatetime())
                    .build();

            // No transformation needed - use the provided sessionLayoutData directly
            // Just validate it's valid JSON matching our DTO structure
            String validatedLayoutData = validateAndPrepareSessionLayout(sessionLayoutData);

            SessionSeatingMap map = SessionSeatingMap.builder()
                    .layoutData(validatedLayoutData)
                    .eventSession(session)
                    .build();

            session.setSessionSeatingMap(map);
            sessions.add(session);
        }
        return sessions;
    }

    /**
     * Validates the session layout data is properly formatted and initializes any required fields
     * Always assigns new unique UUIDs to every block, row, and seat
     *
     * @param sessionLayoutData JSON string containing the session layout
     * @return The validated and possibly enhanced JSON string
     */
    private String validateAndPrepareSessionLayout(String sessionLayoutData) {
        try {
            // Parse the JSON to verify it's valid and matches our expected structure
            LayoutDataDTO layoutData = objectMapper.readValue(sessionLayoutData, LayoutDataDTO.class);

            // Initialize fields and assign new UUIDs
            if (layoutData.getLayout() != null && layoutData.getLayout().getBlocks() != null) {
                layoutData.getLayout().getBlocks().forEach(block -> {
                    // Always generate a new UUID for each block
                    block.setId(UUID.randomUUID().toString());

                    // For seated blocks, make sure rows and seats have IDs and proper initialization
                    if ("seated_grid".equals(block.getType()) && block.getRows() != null) {
                        for (LayoutDataDTO.Row row : block.getRows()) {
                            // Always generate a new UUID for each row
                            row.setId(UUID.randomUUID().toString());

                            if (row.getSeats() != null) {
                                for (LayoutDataDTO.Seat seat : row.getSeats()) {
                                    // Always generate a new UUID for each seat
                                    seat.setId(UUID.randomUUID().toString());
                                    
                                    // Ensure seats have a status set
                                    if (seat.getStatus() == null) {
                                        seat.setStatus("AVAILABLE");
                                    }
                                }
                            }
                        }
                    }

                    // Initialize standing capacity block properties if needed
                    if ("standing_capacity".equals(block.getType())) {
                        if (block.getSoldCount() == null) {
                            block.setSoldCount(0);
                        }
                    }
                });
            }

            // Reserialize the validated object back to JSON
            return objectMapper.writeValueAsString(layoutData);

        } catch (IOException e) {
            log.error("Invalid session layout data", e);
            throw new IllegalArgumentException("Invalid session layout data: " + e.getMessage());
        }
    }

    private Venue findVenue(UUID venueId) {
        if (venueId == null) return null;
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + venueId));
    }

    private Set<Category> findCategories(Set<UUID> categoryIds) {
        return new java.util.HashSet<>(categoryRepository.findAllById(categoryIds));
    }
}