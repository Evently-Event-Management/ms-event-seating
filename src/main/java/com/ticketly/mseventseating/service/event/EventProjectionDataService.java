package com.ticketly.mseventseating.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.VenueDetailsDTO;
import com.ticketly.mseventseating.dto.projection.EventProjectionDTO;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProjectionDataService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public EventProjectionDTO getEventProjectionData(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found for projection: " + eventId));
        return mapToProjectionDTO(event);
    }

    private EventProjectionDTO mapToProjectionDTO(Event event) {
        // This mapping logic builds the rich DTO from the JPA entities
        // It's the bridge between your command model and your read model

        EventProjectionDTO.OrganizationInfo orgInfo = EventProjectionDTO.OrganizationInfo.builder()
                .id(event.getOrganization().getId())
                .name(event.getOrganization().getName())
                .logoUrl(event.getOrganization().getLogoUrl())
                .build();

        EventProjectionDTO.CategoryInfo catInfo = EventProjectionDTO.CategoryInfo.builder()
                .id(event.getCategory().getId())
                .name(event.getCategory().getName())
                .parentName(event.getCategory().getParent() != null ? event.getCategory().getParent().getName() : null)
                .build();

        List<EventProjectionDTO.TierInfo> tierInfo = event.getTiers().stream()
                .map(t -> EventProjectionDTO.TierInfo.builder()
                        .id(t.getId()).name(t.getName()).price(t.getPrice()).color(t.getColor()).build())
                .toList();

        List<EventProjectionDTO.SessionInfo> sessionInfo = event.getSessions().stream()
                .map(s -> {
                    VenueDetailsDTO venueDetails = parseVenueDetails(s.getVenueDetails());
                    return EventProjectionDTO.SessionInfo.builder()
                            .id(s.getId()).startTime(s.getStartTime()).endTime(s.getEndTime())
                            .status(s.getStatus().name()).sessionType(s.getSessionType())
                            .venueDetails(mapToVenueDetailsInfo(venueDetails))
                            .build();
                })
                .toList();

        return EventProjectionDTO.builder()
                .id(event.getId()).title(event.getTitle()).description(event.getDescription())
                .overview(event.getOverview()).status(event.getStatus()).coverPhotos(null) // You'd map S3 keys to URLs here
                .organization(orgInfo).category(catInfo).tiers(Collections.singletonList((EventProjectionDTO.TierInfo) tierInfo)).sessions(Collections.singletonList((EventProjectionDTO.SessionInfo) sessionInfo))
                .build();
    }

    // Helper methods for parsing and mapping
    private VenueDetailsDTO parseVenueDetails(String json) {
        try {
            if (json == null) return null;
            return objectMapper.readValue(json, VenueDetailsDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse venue details JSON", e);
            return null;
        }
    }

    private EventProjectionDTO.VenueDetailsInfo mapToVenueDetailsInfo(VenueDetailsDTO dto) {
        if (dto == null) return null;
        EventProjectionDTO.GeoJsonPoint point = null;
        if (dto.getLongitude() != null && dto.getLatitude() != null) {
            point = EventProjectionDTO.GeoJsonPoint.builder()
                    .coordinates(new double[]{dto.getLongitude(), dto.getLatitude()})
                    .build();
        }
        return EventProjectionDTO.VenueDetailsInfo.builder()
                .name(dto.getName()).address(dto.getAddress()).onlineLink(dto.getOnlineLink()).location(point)
                .build();
    }
}
