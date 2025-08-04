package com.ticketly.mseventseating.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.session_layout.SessionSeatingMapRequest;
import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.session.SessionRequest;
import com.ticketly.mseventseating.dto.tier.TierRequest;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.CategoryRepository;
import com.ticketly.mseventseating.repository.VenueRepository;
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

    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public Event createFromRequest(CreateEventRequest request, Organization organization) {
        Venue venue = findVenue(request.getVenueId());
        Set<Category> categories = findCategories(request.getCategoryIds());

        Event event = buildEventEntity(request, organization, venue, categories);

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
            EventSession session = EventSession.builder()
                    .startTime(req.getStartTime())
                    .endTime(req.getEndTime())
                    .event(event)
                    .salesStartRuleType(req.getSalesStartRuleType())
                    .salesStartHoursBefore(req.getSalesStartHoursBefore())
                    .salesStartFixedDatetime(req.getSalesStartFixedDatetime())
                    .build();

            String validatedLayoutData = validateAndPrepareSessionLayout(req.getSessionSeatingMapRequest(), tierIdMap);

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
                        if (row.getSeats() == null) continue;
                        for (SessionSeatingMapRequest.Seat seat : row.getSeats()) {
                            seat.setId(UUID.randomUUID().toString());
                            seat.setStatus("AVAILABLE");

                            if (seat.getTierId() != null) {
                                Tier realTier = tierIdMap.get(seat.getTierId());
                                if (realTier == null) {
                                    throw new BadRequestException("Seat is assigned to an invalid Tier ID: " + seat.getTierId());
                                }
                                seat.setTierId(realTier.getId().toString());
                            }
                        }
                    }
                }

                if ("standing_capacity".equals(block.getType())) {
                    block.setSoldCount(0);
                    if (block.getTierId() != null) {
                        Tier realTier = tierIdMap.get(block.getTierId());
                        if (realTier == null) {
                            throw new BadRequestException("Block is assigned to an invalid Tier ID: " + block.getTierId());
                        }
                        block.setTierId(realTier.getId().toString());
                    }
                }
            }

            return objectMapper.writeValueAsString(layoutData);

        } catch (IOException e) {
            log.error("Invalid session layout data", e);
            throw new BadRequestException("Invalid session layout data: " + e.getMessage());
        }
    }

    private List<Tier> buildTiers(List<TierRequest> tierRequests, Event event, Map<String, Tier> tierIdMap) {
        return tierRequests.stream()
                .map(req -> {
                    Tier tier = Tier.builder()
                            .id(UUID.randomUUID()) // Generate a new UUID for the tier
                            .name(req.getName())
                            .price(req.getPrice())
                            .color(req.getColor())
                            .event(event)
                            .build();
                    // ✅ Changed from getTempId() to getId()
                    tierIdMap.put(req.getId(), tier);
                    return tier;
                })
                .collect(Collectors.toList());
    }

    // --- Helper methods for finding entities ---
    private Event buildEventEntity(CreateEventRequest request, Organization org, Venue venue, Set<Category> categories) {
        return Event.builder()
                .title(request.getTitle()).description(request.getDescription()).overview(request.getOverview())
                .coverPhotos(request.getCoverPhotos()).organization(org).venue(venue).categories(categories)
                .isOnline(request.isOnline()).onlineLink(request.getOnlineLink())
                .locationDescription(request.getLocationDescription())
                .build();
    }

    private Venue findVenue(UUID venueId) {
        if (venueId == null) return null;
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + venueId));
    }

    private Set<Category> findCategories(Set<UUID> categoryIds) {
        return new HashSet<>(categoryRepository.findAllById(categoryIds));
    }
}
