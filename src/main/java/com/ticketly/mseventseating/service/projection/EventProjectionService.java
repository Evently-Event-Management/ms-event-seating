package com.ticketly.mseventseating.service.projection;

import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventCoverPhoto;
import com.ticketly.mseventseating.model.Tier;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.repository.EventRepository;
import dto.projection.DiscountProjectionDTO;
import dto.projection.EventProjectionDTO;
import dto.projection.SessionProjectionDTO;
import dto.projection.TierInfo;
import lombok.RequiredArgsConstructor;
import model.EventStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventProjectionService {
    private final SessionProjectionService sessionProjectionService;
    private final DiscountProjectionService discountProjectionService;
    private final EventRepository eventRepository;

    public EventProjectionDTO projectEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found for projection: " + eventId));
        if (event.getStatus() != EventStatus.APPROVED && event.getStatus() != EventStatus.COMPLETED) {
            throw new ResourceNotFoundException("Event is not approved for projection: " + event.getId());
        }

        EventProjectionDTO.OrganizationInfo orgInfo = EventProjectionDTO.OrganizationInfo.builder()
                .id(event.getOrganization().getId())
                .name(event.getOrganization().getName())
                .userId(event.getOrganization().getUserId())
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
        Map<UUID, TierInfo> tierInfoMap = tierInfoList.stream()
                .collect(Collectors.toMap(TierInfo::getId, Function.identity()));

        List<SessionProjectionDTO> sessionInfo = event.getSessions().stream()
                .map(session -> sessionProjectionService.projectSession(session, tierInfoMap))
                .collect(Collectors.toList());

        List<DiscountProjectionDTO> discountInfo = event.getDiscounts().stream()
                .map(discountProjectionService::mapToDiscountDetailsDTO)
                .toList();

        return EventProjectionDTO.builder()
                .id(event.getId()).title(event.getTitle()).description(event.getDescription())
                .overview(event.getOverview()).status(event.getStatus()).coverPhotos(event.getCoverPhotos().stream()
                        .map(EventCoverPhoto::getPhotoUrl).collect(Collectors.toList()))
                .organization(orgInfo).category(catInfo)
                .tiers(tierInfoList)
                .sessions(sessionInfo)
                .discounts(discountInfo)
                .build();
    }

    private TierInfo mapToTierInfo(Tier tier) {
        return TierInfo.builder()
                .id(tier.getId()).name(tier.getName()).price(tier.getPrice()).color(tier.getColor())
                .build();
    }
}
