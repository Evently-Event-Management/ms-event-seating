package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.dto.event.VenueDetailsDTO;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.Tier;
import com.ticketly.mseventseating.repository.EventSessionRepository;
import dto.projection.SeatingMapProjectionDTO;
import dto.projection.SessionProjectionDTO;
import dto.projection.TierInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.EventStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionProjectionService {
    private final SeatingMapProjectionService seatingMapProjectionService;
    private final VenueDetailsMapper venueDetailsMapper;
    private final EventSessionRepository eventSessionRepository;

    public SessionProjectionDTO projectSession(EventSession session, Map<UUID, TierInfo> tierInfoMap) {
        VenueDetailsDTO venueDetails = venueDetailsMapper.parseVenueDetails(session.getVenueDetails());
        SeatingMapProjectionDTO layoutData = seatingMapProjectionService.projectSeatingMap(
                session.getSessionSeatingMap().getLayoutData(), tierInfoMap
        );
        return SessionProjectionDTO.builder()
                .id(session.getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .sessionStatus(session.getStatus())
                .sessionType(session.getSessionType())
                .venueDetails(venueDetailsMapper.mapToVenueDetailsInfo(venueDetails))
                .salesStartTime(session.getSalesStartTime())
                .layoutData(layoutData)
                .build();
    }

    public SessionProjectionDTO projectSession(UUID session) {
        EventSession eventSession = eventSessionRepository.findById(session)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found for projection: " + session));
        Event event = eventSession.getEvent();

        if (event == null) {
            log.error("Session {} does not belong to any event", session);
            throw new ResourceNotFoundException("Session does not belong to any event: " + session);
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

        return projectSession(eventSession, tierInfoMap);
    }

    private TierInfo mapToTierInfo(Tier tier) {
        return TierInfo.builder()
                .id(tier.getId()).name(tier.getName()).price(tier.getPrice()).color(tier.getColor())
                .build();
    }
}
