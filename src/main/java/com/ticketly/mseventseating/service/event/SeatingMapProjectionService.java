package com.ticketly.mseventseating.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.SessionSeatingMapDTO;
import com.ticketly.mseventseating.dto.projection.SeatingMapProjectionDTO;
import com.ticketly.mseventseating.dto.projection.TierInfo;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.SessionSeatingMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatingMapProjectionService {
    private final ObjectMapper objectMapper;
    private final SessionSeatingMapRepository seatingMapRepository;

    public SeatingMapProjectionDTO projectSeatingMap(String layoutJson, Map<UUID, TierInfo> tierInfoMap) {
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

    public SeatingMapProjectionDTO projectSeatingMap(UUID seatingMapId) {
        SessionSeatingMap seatingMap = seatingMapRepository.findById(seatingMapId)
                .orElseThrow(() -> new IllegalArgumentException("Seating map not found with ID: " + seatingMapId));

        EventSession eventSession = seatingMap.getEventSession();

        if (eventSession == null) {
            log.warn("Attempted to project seating map for seating map {} without an associated event session", seatingMapId);
            throw new ResourceNotFoundException("Event session not found for seating map: " + seatingMapId);
        }

        Event event = eventSession.getEvent();

        if (event == null) {
            log.warn("Attempted to project seating map for session {} without an associated event", eventSession.getId());
            throw new ResourceNotFoundException("Event not found for session: " + eventSession.getId());
        }

        if (event.getStatus() != EventStatus.APPROVED) {
            log.warn("Attempted to project session for event {} with status {}", event.getId(), event.getStatus());
            throw new ResourceNotFoundException("Event is not approved for session projection: " + event.getId());
        }

        List<TierInfo> tierInfoList = event.getTiers().stream()
                .map(this::mapToTierInfo)
                .toList();
        Map<UUID, TierInfo> tierInfoMap = tierInfoList.stream()
                .collect(Collectors.toMap(TierInfo::getId, Function.identity()));

        return projectSeatingMap(seatingMap.getLayoutData(), tierInfoMap);
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
            // log error if needed
            return null;
        }
    }

    private TierInfo mapToTierInfo(Tier tier) {
        return TierInfo.builder()
                .id(tier.getId()).name(tier.getName()).price(tier.getPrice()).color(tier.getColor())
                .build();
    }
}
