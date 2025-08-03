package com.ticketly.mseventseating.factory;

import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.session.SessionRequest;
import com.ticketly.mseventseating.dto.tier.TierRequest;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.CategoryRepository;
import com.ticketly.mseventseating.repository.SeatingLayoutTemplateRepository;
import com.ticketly.mseventseating.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventFactory {

    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final SeatingLayoutTemplateRepository seatingLayoutTemplateRepository;

    public Event createFromRequest(CreateEventRequest request, Organization organization) {
        Venue venue = findVenue(request.getVenueId());
        Set<Category> categories = findCategories(request.getCategoryIds());
        SeatingLayoutTemplate layoutTemplate = findLayoutTemplate(request.getSeatingLayoutTemplateId());

        Event event = buildEventEntity(request, organization, venue, categories);

        List<Tier> tiers = buildTiers(request.getTiers(), event);
        event.setTiers(tiers);

        List<EventSession> sessions = buildSessions(request.getSessions(), event, layoutTemplate);
        event.setSessions(sessions);

        return event;
    }

    private Event buildEventEntity(CreateEventRequest request, Organization org, Venue venue, Set<Category> categories) {
        return Event.builder()
                .title(request.getTitle()).description(request.getDescription()).overview(request.getOverview())
                .coverPhotos(request.getCoverPhotos()).organization(org).venue(venue).categories(categories)
                .isOnline(request.isOnline()).onlineLink(request.getOnlineLink())
                .locationDescription(request.getLocationDescription())
                // ✅ REMOVED: Sales rules are no longer set here.
                .build();
    }

    private List<Tier> buildTiers(List<TierRequest> tierRequests, Event event) {
        return tierRequests.stream()
                .map(req -> Tier.builder().name(req.getName()).price(req.getPrice()).color(req.getColor()).event(event).build())
                .collect(Collectors.toList());
    }

    private List<EventSession> buildSessions(List<SessionRequest> sessionRequests, Event event, SeatingLayoutTemplate layoutTemplate) {
        List<EventSession> sessions = new ArrayList<>();
        for (SessionRequest req : sessionRequests) {
            // ✅ Build the session with its own sales rules from the request.
            EventSession session = EventSession.builder()
                    .startTime(req.getStartTime())
                    .endTime(req.getEndTime())
                    .event(event)
                    .salesStartRuleType(req.getSalesStartRuleType())
                    .salesStartHoursBefore(req.getSalesStartHoursBefore())
                    .salesStartFixedDatetime(req.getSalesStartFixedDatetime())
                    .build();

            SessionSeatingMap map = SessionSeatingMap.builder().layoutData(layoutTemplate.getLayoutData()).eventSession(session).build();
            session.setSessionSeatingMap(map);
            sessions.add(session);
        }
        return sessions;
    }

    private Venue findVenue(UUID venueId) {
        if (venueId == null) return null;
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + venueId));
    }

    private Set<Category> findCategories(Set<UUID> categoryIds) {
        return new java.util.HashSet<>(categoryRepository.findAllById(categoryIds));
    }

    private SeatingLayoutTemplate findLayoutTemplate(UUID templateId) {
        return seatingLayoutTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Seating layout template not found with ID: " + templateId));
    }
}
