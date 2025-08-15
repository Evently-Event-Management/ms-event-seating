package com.ticketly.mseventseating.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.SessionSeatingMapDTO;
import com.ticketly.mseventseating.dto.event.VenueDetailsDTO;
import com.ticketly.mseventseating.dto.projection.EventProjectionDTO;
import com.ticketly.mseventseating.dto.projection.SeatingMapProjectionDTO;
import com.ticketly.mseventseating.dto.projection.SessionProjectionDTO;
import com.ticketly.mseventseating.dto.projection.TierInfo;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventStatus;
import com.ticketly.mseventseating.model.Tier;
import com.ticketly.mseventseating.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        if (event.getStatus() != EventStatus.APPROVED) {
            log.warn("Attempted to fetch projection data for event {} with status {}", eventId, event.getStatus());
            throw new ResourceNotFoundException("Event is not approved for projection: " + eventId);
        }

        return mapToProjectionDTO(event);
    }

    private EventProjectionDTO mapToProjectionDTO(Event event) {
        // --- Map top-level details ---
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

        List<TierInfo> tierInfoList = event.getTiers().stream()
                .map(this::mapToTierInfo)
                .collect(Collectors.toList());

        // Create a lookup map for efficient tier embedding
        Map<UUID, TierInfo> tierInfoMap = tierInfoList.stream()
                .collect(Collectors.toMap(TierInfo::getId, Function.identity()));

        // --- Map Sessions with Denormalized Seating Maps ---
        List<SessionProjectionDTO> sessionInfo = event.getSessions().stream()
                .map(s -> {
                    VenueDetailsDTO venueDetails = parseVenueDetails(s.getVenueDetails());
                    SeatingMapProjectionDTO layoutData = mapToSessionSeatingMapInfo(
                            s.getSessionSeatingMap().getLayoutData(),
                            tierInfoMap
                    );

                    return SessionProjectionDTO.builder()
                            .id(s.getId()).startTime(s.getStartTime()).endTime(s.getEndTime())
                            .status(s.getStatus().name()).sessionType(s.getSessionType())
                            .venueDetails(mapToVenueDetailsInfo(venueDetails))
                            .layoutData(layoutData)
                            .build();
                })
                .collect(Collectors.toList());

        return EventProjectionDTO.builder()
                .id(event.getId()).title(event.getTitle()).description(event.getDescription())
                .overview(event.getOverview()).status(event.getStatus()).coverPhotos(null) // Map S3 keys to URLs here
                .organization(orgInfo).category(catInfo)
                .tiers(tierInfoList) // Include the summary of all tiers
                .sessions(sessionInfo)
                .build();
    }

    /**
     * The core denormalization logic. It takes the seating map JSON and the tier lookup map,
     * then builds a new seating map object with the full tier details embedded in each seat.
     */
    private SeatingMapProjectionDTO mapToSessionSeatingMapInfo(String layoutJson, Map<UUID, TierInfo> tierInfoMap) {
        SessionSeatingMapDTO sourceDto = parseLayoutData(layoutJson);
        if (sourceDto == null) return null;

        List<SeatingMapProjectionDTO.BlockInfo> blockInfos = sourceDto.getLayout().getBlocks().stream()
                .map(blockDto -> {
                    List<SeatingMapProjectionDTO.RowInfo> rowInfos = blockDto.getRows() != null ? blockDto.getRows().stream()
                            .map(rowDto -> SeatingMapProjectionDTO.RowInfo.builder()
                                    .id(rowDto.getId())
                                    .label(rowDto.getLabel())
                                    .seats(mapSeatsWithTiers(rowDto.getSeats(), tierInfoMap))
                                    .build())
                            .collect(Collectors.toList()) : null;

                    List<SeatingMapProjectionDTO.SeatInfo> seatInfos = blockDto.getSeats() != null ?
                            mapSeatsWithTiers(blockDto.getSeats(), tierInfoMap) : null;

                    return SeatingMapProjectionDTO.BlockInfo.builder()
                            .id(blockDto.getId()).name(blockDto.getName()).type(blockDto.getType())
                            .position(SeatingMapProjectionDTO.PositionInfo.builder()
                                    .x(blockDto.getPosition().getX())
                                    .y(blockDto.getPosition().getY())
                                    .build())
                            .rows(rowInfos).seats(seatInfos).capacity(blockDto.getCapacity())
                            .width(blockDto.getWidth()).height(blockDto.getHeight())
                            .build();
                }).collect(Collectors.toList());

        return SeatingMapProjectionDTO.builder()
                .name(sourceDto.getName())
                .layout(SeatingMapProjectionDTO.LayoutInfo.builder().blocks(blockInfos).build())
                .build();
    }

    private List<SeatingMapProjectionDTO.SeatInfo> mapSeatsWithTiers(List<SessionSeatingMapDTO.Seat> seatDtos, Map<UUID, TierInfo> tierInfoMap) {
        return seatDtos.stream().map(seatDto -> {
            TierInfo embeddedTier = seatDto.getTierId() != null
                    ? tierInfoMap.get(UUID.fromString(seatDto.getTierId()))
                    : null;
            return SeatingMapProjectionDTO.SeatInfo.builder()
                    .id(seatDto.getId())
                    .label(seatDto.getLabel())
                    .status(seatDto.getStatus())
                    .tier(embeddedTier)
                    .build();
        }).collect(Collectors.toList());
    }

    private SessionSeatingMapDTO parseLayoutData(String json) {
        try {
            if (json == null) return null;
            return objectMapper.readValue(json, SessionSeatingMapDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse session seating map JSON", e);
            return null;
        }
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

    private SessionProjectionDTO.VenueDetailsInfo mapToVenueDetailsInfo(VenueDetailsDTO dto) {
        if (dto == null) return null;
        SessionProjectionDTO.GeoJsonPoint point = null;
        if (dto.getLongitude() != null && dto.getLatitude() != null) {
            point = SessionProjectionDTO.GeoJsonPoint.builder()
                    .coordinates(new double[]{dto.getLongitude(), dto.getLatitude()})
                    .build();
        }
        return SessionProjectionDTO.VenueDetailsInfo.builder()
                .name(dto.getName()).address(dto.getAddress()).onlineLink(dto.getOnlineLink()).location(point)
                .build();
    }

    private TierInfo mapToTierInfo(Tier tier) {
        return TierInfo.builder()
                .id(tier.getId()).name(tier.getName()).price(tier.getPrice()).color(tier.getColor())
                .build();
    }
}
