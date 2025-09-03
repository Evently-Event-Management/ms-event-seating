package com.ticketly.mseventseating.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.*;
import com.ticketly.mseventseating.service.event.EventCreationService;
import com.ticketly.mseventseating.service.event.EventLifecycleService;
import com.ticketly.mseventseating.service.event.EventQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventCreationService eventCreationService;
    private final EventLifecycleService eventLifecycleService;
    private final EventQueryService eventQueryService;
    private final ObjectMapper objectMapper;

    // ✅ Updated endpoint to consume multipart/form-data
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<EventResponseDTO> createEvent(
            @RequestPart("request") @Valid String requestStr,
            @RequestPart(value = "coverImages", required = false) MultipartFile[] coverImages,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        // Convert the JSON string part into our DTO
        CreateEventRequest request = objectMapper.readValue(requestStr, CreateEventRequest.class);

        String userId = jwt.getSubject();
        EventResponseDTO createdEvent = eventCreationService.createEvent(request, coverImages, userId, jwt);
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }

    /**
     * Get detailed information about a specific event with ownership verification
     * Intended for organization owners to access their events
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailDTO> getEventDetailsOwner(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal Jwt jwt) {


        EventDetailDTO event = eventQueryService.findEventByIdOwner(eventId, jwt.getSubject());
        return ResponseEntity.ok(event);
    }

    /**
     * Admin endpoint to get detailed information about any event
     * Bypasses ownership verification as admins can view all events
     */
    @GetMapping("/admin/{eventId}")
    @PreAuthorize("hasRole('event_admin')")
    public ResponseEntity<EventDetailDTO> getEventDetails(
            @PathVariable UUID eventId) {

        EventDetailDTO event = eventQueryService.findEventById(eventId);
        return ResponseEntity.ok(event);
    }

    /**
     * Delete an event - only possible for events with PENDING status
     * Ownership verification is performed in the service layer
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal Jwt jwt) {

        eventLifecycleService.deleteEvent(eventId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get paginated list of events for a specific organization with filtering, searching, and sorting
     * Ownership verification ensures users can only see organizations they have access to
     */
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<Page<EventSummaryDTO>> getOrganizationEventsOwner(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        Page<EventSummaryDTO> events = eventQueryService.findEventsByOrganizationOwner(
                organizationId, jwt.getSubject(), status, search, pageable
        );
        return ResponseEntity.ok(events);
    }

    /**
     * Admin endpoint to get paginated list of events for any organization
     * Bypasses ownership verification as admins can view all organizations' events
     */
    @GetMapping("/admin/organization/{organizationId}")
    @PreAuthorize("hasRole('event_admin')")
    public ResponseEntity<Page<EventSummaryDTO>> getOrganizationEvents(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        Page<EventSummaryDTO> events = eventQueryService.findEventsByOrganization(
                organizationId, status, search, pageable
        );
        return ResponseEntity.ok(events);
    }

    /**
     * Admin endpoint to get paginated list of all events across all organizations
     * Only accessible by users with the event_admin role
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('event_admin')")
    public ResponseEntity<Page<EventSummaryDTO>> getAllEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<EventSummaryDTO> events = eventQueryService.findAllEvents(status, search, pageable);
        return ResponseEntity.ok(events);
    }


    /**
     * Endpoint to approve an event - only accessible by users with the event_admin role
     */
    @PostMapping("/{eventId}/approve")
    @PreAuthorize("hasRole('event_admin')")
    public ResponseEntity<Void> approveEvent(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal Jwt jwt) {

        eventLifecycleService.approveEvent(eventId, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint to reject an event - only accessible by users with the event_admin role
     */
    @PostMapping("/{eventId}/reject")
    @PreAuthorize("hasRole('event_admin')")
    public ResponseEntity<Void> rejectEvent(
            @PathVariable UUID eventId,
            @RequestBody RejectEventRequest request) {

        eventLifecycleService.rejectEvent(eventId, request.getReason());
        return ResponseEntity.ok().build();
    }



}
