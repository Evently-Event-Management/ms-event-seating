package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.factory.EventFactory;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.OrganizationOwnershipService;
import com.ticketly.mseventseating.service.SubscriptionTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCreationService {

    private final EventRepository eventRepository;
    private final OrganizationOwnershipService ownershipService;
    private final SubscriptionTierService tierService;
    private final EventFactory eventFactory;


    @Transactional
    public EventResponseDTO createEvent(CreateEventRequest request, String userId, Jwt jwt) {
        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(request.getOrganizationId(), userId);
        validateTierLimits(organization.getId(), jwt, request.getSessions().size());

        Event event = eventFactory.createFromRequest(request, organization);

        Event savedEvent = eventRepository.save(event);
        log.info("Created new PENDING event with ID: {}", savedEvent.getId());

        // Note: Scheduling is NOT done here anymore. It's done upon APPROVAL.

        return mapToEventResponseDTO(savedEvent);
    }

    private void validateTierLimits(UUID organizationId, Jwt jwt, int numSessions) {
        int maxActiveEvents = tierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        long currentActiveEvents = eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
        if (currentActiveEvents >= maxActiveEvents) {
            throw new BadRequestException("You have reached the limit of " + maxActiveEvents + " active events for your tier.");
        }

        int maxSessions = tierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        if (numSessions > maxSessions) {
            throw new BadRequestException("You cannot create more than " + maxSessions + " sessions per event for your tier.");
        }
    }

    private EventResponseDTO mapToEventResponseDTO(Event event) {
        return EventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .status(event.getStatus().name())
                .organizationId(event.getOrganization().getId())
                .createdAt(event.getCreatedAt())
                .build();
    }
}