package com.ticketly.mseventseating.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.SessionRequest;
import com.ticketly.mseventseating.dto.event.SessionSeatingMapRequest;
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

    public Event createFromRequest(CreateEventRequest request, Organization organization, List<String> coverPhotoKeys) {
        Category category = findCategory(request.getCategoryId());

        Event event = buildEventEntity(request, organization, category, coverPhotoKeys);

        Map<String, Tier> tierIdMap = new HashMap<>();
        List<Tier> tiers = buildTiers(request.getTiers(), event, tierIdMap);
        event.setTiers(tiers);

        List<EventSession> sessions = buildSessions(request.getSessions(), event, tierIdMap);
        event.setSessions(sessions);

        return event;
    }

    private List<EventSession> buildSessions(List<SessionRequest> sessionRequests, Event event, Map<String, Tier> tierIdMap) {
        List<EventSession> sessions = new ArrayList<>();
        for (SessionRequest req : sessionRequests) {
            String venueDetailsJson = null;
            if (!req.isOnline() && req.getVenueDetails() != null) {
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
                    .isOnline(req.isOnline())
                    .onlineLink(req.getOnlineLink())
                    .venueDetails(venueDetailsJson)
                    .salesStartRuleType(req.getSalesStartRuleType())
                    .salesStartHoursBefore(req.getSalesStartHoursBefore())
                    .salesStartFixedDatetime(req.getSalesStartFixedDatetime())
                    .build();

            String validatedLayoutData = validateAndPrepareSessionLayout(req.getLayoutData(), tierIdMap);

            SessionSeatingMap map = SessionSeatingMap.builder()
                    .layoutData(validatedLayoutData)
                    .eventSession(session)
                    .build();

            session.setSessionSeatingMap(map);
            sessions.add(session);
        }
        return sessions;
    }

    private String validateAndPrepareSessionLayout(SessionSeatingMapRequest layoutData, Map<String, Tier> tierIdMap) {
        try {
            if (layoutData == null || layoutData.getLayout() == null || layoutData.getLayout().getBlocks() == null) {
                throw new BadRequestException("Layout data or blocks cannot be null.");
            }

            for (SessionSeatingMapRequest.Block block : layoutData.getLayout().getBlocks()) {
                block.setId(UUID.randomUUID().toString());

                if ("seated_grid".equals(block.getType())) {
                    if (block.getRows() == null) continue;
                    for (SessionSeatingMapRequest.Row row : block.getRows()) {
                        row.setId(UUID.randomUUID().toString());
                        if (row.getSeats() != null) {
                            validateAndPrepareSeats(row.getSeats(), tierIdMap);
                        }
                    }
                } else if ("standing_capacity".equals(block.getType())) {
                    // ✅ Process the flat list of seats for capacity blocks
                    if (block.getSeats() != null) {
                        validateAndPrepareSeats(block.getSeats(), tierIdMap);
                    }
                    // ✅ Nullify fields that are no longer the source of truth for this block type
                    block.setSoldCount(null);
                    block.setTierId(null);
                }
            }

            return objectMapper.writeValueAsString(layoutData);

        } catch (IOException e) {
            log.error("Invalid session layout data", e);
            throw new BadRequestException("Invalid session layout data: " + e.getMessage());
        }
    }

    /**
     * Helper method to process a list of seats, assigning UUIDs and validating tiers.
     * This is now used by both seated_grid and standing_capacity blocks.
     */
    private void validateAndPrepareSeats(List<SessionSeatingMapRequest.Seat> seats, Map<String, Tier> tierIdMap) {
        for (SessionSeatingMapRequest.Seat seat : seats) {
            seat.setId(UUID.randomUUID().toString());

            if (!"RESERVED".equals(seat.getStatus())) {
                seat.setStatus("AVAILABLE");
            }

            if (seat.getTierId() != null) {
                Tier realTier = tierIdMap.get(seat.getTierId());
                if (realTier == null) {
                    throw new BadRequestException("Seat/slot is assigned to an invalid Tier ID: " + seat.getTierId());
                }
                seat.setTierId(realTier.getId().toString());
            }
        }
    }

    private List<Tier> buildTiers(List<TierRequest> tierRequests, Event event, Map<String, Tier> tierIdMap) {
        return tierRequests.stream()
                .map(req -> {
                    Tier tier = Tier.builder()
                            .name(req.getName())
                            .price(req.getPrice())
                            .color(req.getColor())
                            .event(event)
                            .build();
                    tierIdMap.put(req.getId(), tier);
                    return tier;
                })
                .collect(Collectors.toList());
    }

    // --- Helper methods for finding entities ---
    private Event buildEventEntity(CreateEventRequest request, Organization org, Category category, List<String> coverPhotoKeys) {
        return Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .overview(request.getOverview())
                .coverPhotos(coverPhotoKeys)
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