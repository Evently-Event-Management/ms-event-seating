package com.ticketly.mseventseating.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.SessionRequest;
import com.ticketly.mseventseating.dto.event.SessionSeatingMapDTO;
import com.ticketly.mseventseating.dto.event.TierRequest;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventFactory {

    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Assembles the complete Event aggregate (Event, Tiers, Sessions, Maps) in memory
     * before it is persisted.
     */
    public Event createFromRequest(CreateEventRequest request, Organization organization, List<String> coverPhotoUrls) {
        Category category = findCategory(request.getCategoryId());
        Event event = buildEventEntity(request, organization, category);

        // Create cover photo entities
        List<EventCoverPhoto> coverPhotos = createCoverPhotos(coverPhotoUrls, event);
        event.setCoverPhotos(coverPhotos);

        // This map will translate the client's temporary tier IDs to the newly created Tier objects.
        Map<String, Tier> tierIdMap = new HashMap<>();
        List<Tier> tiers = buildTiers(request.getTiers(), event, tierIdMap);
        event.setTiers(tiers);

        List<EventSession> sessions = buildSessions(request.getSessions(), event, tierIdMap);
        event.setSessions(sessions);

        return event;
    }

    /**
     * Creates EventCoverPhoto entities for each uploaded photo URL
     */
    private List<EventCoverPhoto> createCoverPhotos(List<String> photoUrls, Event event) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            return new ArrayList<>();
        }

        return photoUrls.stream()
                .map(url -> EventCoverPhoto.builder()
                        .photoUrl(url)
                        .event(event)
                        .build())
                .collect(Collectors.toList());
    }

    private List<Tier> buildTiers(List<TierRequest> tierRequests, Event event, Map<String, Tier> tierIdMap) {
        return tierRequests.stream()
                .map(req -> {
                    Tier tier = Tier.builder()
                            .id(UUID.randomUUID())
                            .name(req.getName())
                            .price(req.getPrice())
                            .color(req.getColor())
                            .event(event)
                            .build();
                    // Map the client's temp ID to the fully formed Tier object (which now has a permanent ID)
                    tierIdMap.put(req.getId(), tier);
                    return tier;
                })
                .collect(Collectors.toList());
    }

    private List<EventSession> buildSessions(List<SessionRequest> sessionRequests, Event event, Map<String, Tier> tierIdMap) {
        List<EventSession> sessions = new ArrayList<>();
        for (SessionRequest req : sessionRequests) {
            // Serialize the venueDetails DTO to a JSON string.
            // This object now contains either the online link or physical address.
            String venueDetailsJson = null;
            if (req.getVenueDetails() != null) {
                try {
                    venueDetailsJson = objectMapper.writeValueAsString(req.getVenueDetails());
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize venue details for session", e);
                    throw new BadRequestException("Invalid venue details format.");
                }
            }

            EventSession session = EventSession.builder()
                    .startTime(req.getStartTime())
                    .endTime(req.getEndTime())
                    .event(event)
                    .sessionType(req.getSessionType()) // Use the new enum
                    .venueDetails(venueDetailsJson) // Set the JSON string
                    .salesStartRuleType(req.getSalesStartRuleType())
                    .salesStartHoursBefore(req.getSalesStartHoursBefore())
                    .salesStartFixedDatetime(req.getSalesStartFixedDatetime())
                    .build();

            String validatedLayoutData = prepareSessionLayout(req.getLayoutData(), tierIdMap);

            SessionSeatingMap map = SessionSeatingMap.builder()
                    .layoutData(validatedLayoutData)
                    .eventSession(session)
                    .build();

            session.setSessionSeatingMap(map);
            sessions.add(session);
        }
        return sessions;
    }

    private String prepareSessionLayout(SessionSeatingMapDTO layoutData, Map<String, Tier> tierIdMap) {
        try {
            if (layoutData == null || layoutData.getLayout() == null || layoutData.getLayout().getBlocks() == null) {
                throw new BadRequestException("Layout data or blocks cannot be null.");
            }

            for (SessionSeatingMapDTO.Block block : layoutData.getLayout().getBlocks()) {
                block.setId(UUID.randomUUID().toString());

                if ("seated_grid".equals(block.getType())) {
                    if (block.getRows() == null) continue;
                    for (SessionSeatingMapDTO.Row row : block.getRows()) {
                        row.setId(UUID.randomUUID().toString());
                        if (row.getSeats() != null) {
                            prepareSeats(row.getSeats(), tierIdMap);
                        }
                    }
                } else if ("standing_capacity".equals(block.getType())) {
                    if (block.getSeats() != null) {
                        prepareSeats(block.getSeats(), tierIdMap);
                    }
                }
            }
            return objectMapper.writeValueAsString(layoutData);
        } catch (IOException e) {
            log.error("Invalid session layout data", e);
            throw new BadRequestException("Invalid session layout data: " + e.getMessage());
        }
    }

    private void prepareSeats(List<SessionSeatingMapDTO.Seat> seats, Map<String, Tier> tierIdMap) {
        for (SessionSeatingMapDTO.Seat seat : seats) {
            seat.setId(UUID.randomUUID().toString());
            seat.setStatus("AVAILABLE"); // Always initialize as AVAILABLE

            if (seat.getTierId() != null) {
                Tier realTier = tierIdMap.get(seat.getTierId());
                if (realTier == null) {
                    throw new BadRequestException("Seat/slot is assigned to an invalid Tier ID: " + seat.getTierId());
                }
                // Now we can use the permanent ID from the Tier object
                seat.setTierId(realTier.getId().toString());
            }
        }
    }

    // --- Helper methods for finding entities ---
    private Event buildEventEntity(CreateEventRequest request, Organization org, Category category) {
        return Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .overview(request.getOverview())
                .organization(org)
                .category(category)
                .build();
    }

    private Category findCategory(UUID categoryId) {
        if (categoryId == null) {
            throw new BadRequestException("Category ID is required.");
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
    }
}
