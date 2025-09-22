package com.ticketly.mseventseating.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.*;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.model.discount.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.organization.OrganizationOwnershipService;
import com.ticketly.mseventseating.service.storage.S3StorageService;
import dto.SessionSeatingMapDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.DiscountType;
import model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventQueryService {

    private final EventRepository eventRepository;
    private final EventOwnershipService eventOwnershipService;
    private final ObjectMapper objectMapper;
    private final OrganizationOwnershipService ownershipService;
    private final S3StorageService s3StorageService;

    /**
     * Finds all events with optional status filtering and search term
     * This method is typically used by admin users who can view all events across organizations
     *
     * @param status     Filter by status (optional)
     * @param searchTerm Search in title and description (optional)
     * @param pageable   Pagination information
     * @return Page of event summaries
     */
    @Transactional(readOnly = true)
    public Page<EventSummaryDTO> findAllEvents(EventStatus status, String searchTerm, Pageable pageable) {
        log.info("Admin query: Finding all events with status: {}, searchTerm: '{}', page: {}, size: {}",
                status, searchTerm, pageable.getPageNumber(), pageable.getPageSize());
        Page<Event> eventPage;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // If search term is provided, use the search method
            eventPage = eventRepository.findBySearchTermAndStatus(searchTerm.trim(), status, pageable);
            log.debug("Admin search with term: '{}'", searchTerm.trim());
        } else if (status != null) {
            // If only status filter is provided
            eventPage = eventRepository.findAllByStatus(status, pageable);
            log.debug("Admin filtering by status: {}", status);
        } else {
            // No filters
            eventPage = eventRepository.findAll(pageable);
            log.debug("Admin retrieving all events without filters");
        }

        Page<EventSummaryDTO> result = eventPage.map(this::mapToEventSummary);
        log.debug("Admin query found {} events in page {} matching criteria", result.getNumberOfElements(), result.getNumber());
        return result;
    }

    /**
     * Finds a specific event by ID with all its details
     * Performs ownership verification to ensure the user has permission to access this event
     *
     * @param eventId Event ID
     * @param userId  The ID of the requesting user
     * @return Detailed event information
     * @throws AuthorizationDeniedException if the user doesn't own the event
     * @throws ResourceNotFoundException    if the event doesn't exist
     */
    @Transactional(readOnly = true)
    public EventDetailDTO findEventByIdOwner(UUID eventId, String userId) {
        log.info("User {} requesting event details for ID: {}", userId, eventId);

        // 1. Perform the fast, cached ownership check first.
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("Access denied: User {} is not authorized to access event {}", userId, eventId);
            throw new AuthorizationDeniedException("You don't have permission to access this event");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        log.debug("User {} successfully retrieved owned event: {}", userId, eventId);
        return mapToEventDetail(event);
    }

    /**
     * Finds a specific event by ID with all its details without authorization checks
     * This method is intended for admin-level access only
     *
     * @param eventId Event ID
     * @return Detailed event information
     * @throws ResourceNotFoundException if the event doesn't exist
     */
    @Transactional(readOnly = true)
    public EventDetailDTO findEventById(UUID eventId) {
        log.info("Admin query: Finding event details for ID: {}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Event not found with ID: {}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });

        log.debug("Admin successfully retrieved event: {} with title: '{}'", eventId, event.getTitle());
        return mapToEventDetail(event);
    }

    /**
     * Finds events for a specific organization with ownership verification
     * Ensures the requesting user has permission to access this organization's events
     *
     * @param organizationId The organization ID
     * @param userId         The ID of the requesting user
     * @param status         Filter by status (optional)
     * @param searchTerm     Search in title and description (optional)
     * @param pageable       Pagination information
     * @return Page of event summaries
     * @throws AuthorizationDeniedException if the user doesn't have access to the organization
     */
    @Transactional(readOnly = true)
    public Page<EventSummaryDTO> findEventsByOrganizationOwner(UUID organizationId, String userId, EventStatus status, String searchTerm, Pageable pageable) {
        log.info("User {} requesting events for organization: {}, status: {}, searchTerm: '{}'",
                userId, organizationId, status, searchTerm);

        log.debug("Verifying organization ownership for user: {} on organization: {}", userId, organizationId);
        // Verify ownership using the cached method
        if (!ownershipService.isOwner(organizationId, userId)) {
            log.warn("Access denied: User {} is not authorized to access organization {}", userId, organizationId);
            throw new AuthorizationDeniedException("User does not have access to this organization");
        }

        return getEventSummaryDTOS(organizationId, status, searchTerm, pageable);
    }


    /**
     * Finds events for a specific organization without ownership verification
     * This method is intended for admin-level access only
     *
     * @param organizationId The organization ID
     * @param status         Filter by status (optional)
     * @param searchTerm     Search in title and description (optional)
     * @param pageable       Pagination information
     * @return Page of event summaries
     */
    @Transactional(readOnly = true)
    public Page<EventSummaryDTO> findEventsByOrganization(UUID organizationId, EventStatus status, String searchTerm, Pageable pageable) {
        log.info("Admin query: Finding events for organization: {} status: {}, searchTerm: '{}'",
                organizationId, status, searchTerm);

        return getEventSummaryDTOS(organizationId, status, searchTerm, pageable);
    }

    private Page<EventSummaryDTO> getEventSummaryDTOS(UUID organizationId, EventStatus status, String searchTerm, Pageable pageable) {
        searchTerm = (searchTerm != null && !searchTerm.trim().isEmpty())
                ? searchTerm.trim()
                : null;

        Page<Event> eventPage = eventRepository.findByOrganizationIdAndSearchTermAndStatus(
                organizationId,
                searchTerm != null ? searchTerm.trim() : null,
                status,
                pageable
        );

        Page<EventSummaryDTO> result = eventPage.map(this::mapToEventSummary);
        log.debug("Found {} events in page {} for organization {}",
                result.getNumberOfElements(), result.getNumber(), organizationId);
        return result;
    }


    /**
     * Maps an Event entity to EventSummaryDTO for list view
     */
    private EventSummaryDTO mapToEventSummary(Event event) {
        // Find the earliest session
        OffsetDateTime earliestDate = event.getSessions().stream()
                .map(EventSession::getStartTime)
                .min(Comparator.naturalOrder())
                .orElse(null);

        String coverPhotoUrl = null;
        if (event.getCoverPhotos() != null && !event.getCoverPhotos().isEmpty()) {
            // Get first cover photo URL and generate presigned URL
            coverPhotoUrl = s3StorageService.generatePresignedUrl(
                    event.getCoverPhotos().getFirst().getPhotoUrl(), 60);
        }

        return EventSummaryDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .status(event.getStatus())
                .organizationName(event.getOrganization().getName())
                .organizationId(event.getOrganization().getId())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .description(event.getDescription() != null
                        ? (event.getDescription().length() > 150
                        ? event.getDescription().substring(0, 147) + "..."
                        : event.getDescription())
                        : null)
                .coverPhoto(coverPhotoUrl)
                .sessionCount(event.getSessions().size())
                .earliestSessionDate(earliestDate)
                .build();
    }

    /**
     * Maps an Event entity to EventDetailDTO with all nested data
     */
    private EventDetailDTO mapToEventDetail(Event event) {
        log.debug("Mapping event {} to detailed DTO with {} tiers and {} sessions",
                event.getId(), event.getTiers().size(), event.getSessions().size());
        List<TierDTO> tierDTOs = event.getTiers().stream()
                .map(this::mapToTierDTO)
                .collect(Collectors.toList());

        List<SessionDetailDTO> sessionDTOs = event.getSessions().stream()
                .map(this::mapToSessionDetailDTO)
                .collect(Collectors.toList());

        // Map cover photo entities to URLs
        List<String> coverPhotoUrls = event.getCoverPhotos() != null
                ? event.getCoverPhotos().stream()
                .map(photo -> s3StorageService.generatePresignedUrl(photo.getPhotoUrl(), 60))
                .collect(Collectors.toList())
                : null;

        List<DiscountDetailsDTO> discountDTOs = event.getDiscounts() != null
                ? event.getDiscounts().stream()
                .map(this::mapToDiscountDetailsDTO)
                .toList()
                : null;

        return EventDetailDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .overview(event.getOverview())
                .status(event.getStatus())
                .rejectionReason(event.getRejectionReason())
                .coverPhotos(coverPhotoUrls)
                .organizationId(event.getOrganization().getId())
                .organizationName(event.getOrganization().getName())
                .categoryId(event.getCategory().getId())
                .categoryName(event.getCategory().getName())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .tiers(tierDTOs)
                .sessions(sessionDTOs)
                .discounts(discountDTOs)
                .build();
    }

    private TierDTO mapToTierDTO(Tier tier) {
        return TierDTO.builder()
                .id(tier.getId())
                .name(tier.getName())
                .color(tier.getColor())
                .price(tier.getPrice())
                .build();
    }

    private SessionDetailDTO mapToSessionDetailDTO(EventSession session) {
        SessionSeatingMapDTO layoutData = null;
        VenueDetailsDTO venueDetails = null;

        try {
            if (session.getSessionSeatingMap() != null && session.getSessionSeatingMap().getLayoutData() != null) {
                layoutData = objectMapper.readValue(
                        session.getSessionSeatingMap().getLayoutData(),
                        SessionSeatingMapDTO.class);
            }

            if (session.getVenueDetails() != null) {
                venueDetails = objectMapper.readValue(session.getVenueDetails(), VenueDetailsDTO.class);
            }
        } catch (IOException e) {
            log.error("Error parsing JSON data for session {}", session.getId(), e);
        }

        return SessionDetailDTO.builder()
                .id(session.getId())
                .sessionType(session.getSessionType())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .venueDetails(venueDetails)
                .salesStartTime(session.getSalesStartTime())
                .status(session.getStatus())
                .layoutData(layoutData)
                .build();
    }

    private DiscountDetailsDTO mapToDiscountDetailsDTO(Discount discount) {
        return DiscountDetailsDTO.builder()
                .id(discount.getId())
                .code(discount.getCode())
                .parameters(mapDiscountParameters(discount.getParameters()))
                .maxUsage(discount.getMaxUsage())
                .currentUsage(discount.getCurrentUsage())
                .isActive(discount.isActive())
                .isPublic(discount.isPublic())
                .activeFrom(discount.getActiveFrom())
                .expiresAt(discount.getExpiresAt())
                .applicableTierIds(discount.getApplicableTiers() != null
                        ? discount.getApplicableTiers().stream()
                        .map(Tier::getId)
                        .collect(Collectors.toList())
                        : null)
                .applicableSessionIds(discount.getApplicableSessions() != null
                        ? discount.getApplicableSessions().stream()
                        .map(EventSession::getId)
                        .collect(Collectors.toList())
                        : null)
                .build();
    }

    /**
     * Maps domain DiscountParameters to DTO DiscountParametersDTO based on their type
     */
    private DiscountParametersDTO mapDiscountParameters(DiscountParameters parameters) {
        if (parameters == null) {
            return null;
        }

        return switch (parameters) {
            case PercentageDiscountParams p ->
                    new PercentageDiscountParamsDTO(DiscountType.PERCENTAGE, p.percentage());
            case FlatOffDiscountParams f ->
                new FlatOffDiscountParamsDTO(DiscountType.FLAT_OFF, f.amount(), f.currency());
            case BogoDiscountParams b ->
                new BogoDiscountParamsDTO(DiscountType.BUY_N_GET_N_FREE, b.buyQuantity(), b.getQuantity());
        };
    }
}
