package com.ticketly.mseventseating.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.*;
import com.ticketly.mseventseating.model.EventStatus;
import com.ticketly.mseventseating.service.event.EventCreationService;
import com.ticketly.mseventseating.service.event.EventLifecycleService;
import com.ticketly.mseventseating.service.event.EventQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final ObjectMapper objectMapper; // To convert JSON string to DTO

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

    /**
     * Get paginated list of events with filtering and sorting capabilities
     */
    @GetMapping
    @PreAuthorize("hasRole('event_admin')")
    public ResponseEntity<Page<EventSummaryDTO>> getAllEvents(
            @RequestParam(required = false) EventStatus status,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<EventSummaryDTO> events = eventQueryService.findAllEvents(status, pageable);
        return ResponseEntity.ok(events);
    }

    /**
     * Get detailed information about a specific event
     * Authorization is handled in the service layer
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailDTO> getEventDetails(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal Jwt jwt) {

        // Pass the JWT to the service layer for authorization
        boolean isAdmin = jwt.getClaimAsMap("realm_access") != null &&
                jwt.getClaimAsMap("realm_access").containsKey("roles") &&
                jwt.getClaimAsMap("realm_access").get("roles") instanceof Iterable<?> &&
                ((Iterable<?>) jwt.getClaimAsMap("realm_access").get("roles")).toString().contains("event_admin");

        EventDetailDTO event = eventQueryService.findEventById(eventId, jwt.getSubject(), isAdmin);
        return ResponseEntity.ok(event);
    }

    /**
     * Delete an event - only possible for events with PENDING status
     * Authorization is handled in the service layer
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal Jwt jwt) {

        eventLifecycleService.deleteEvent(eventId, jwt);
        return ResponseEntity.noContent().build();
    }
}
