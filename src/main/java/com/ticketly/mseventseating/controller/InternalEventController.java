package com.ticketly.mseventseating.controller;


import com.ticketly.mseventseating.dto.event.SeatDetailsRequest;
import com.ticketly.mseventseating.dto.event.SeatDetailsResponse;
import com.ticketly.mseventseating.service.CategoryProjectionDataService;
import com.ticketly.mseventseating.service.event.EventLifecycleService;
import com.ticketly.mseventseating.service.event.EventProjectionService;
import com.ticketly.mseventseating.service.event.SeatingMapProjectionService;
import com.ticketly.mseventseating.service.event.SeatValidationService;
import com.ticketly.mseventseating.service.event.SessionProjectionService;
import dto.projection.CategoryProjectionDTO;
import dto.projection.EventProjectionDTO;
import dto.projection.SeatingMapProjectionDTO;
import dto.projection.SessionProjectionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1") // A dedicated path for internal M2M calls
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_internal-api')")
public class InternalEventController {

    private final EventLifecycleService eventLifecycleService;
    private final EventProjectionService eventProjectionService;
    private final SessionProjectionService sessionProjectionService;
    private final SeatingMapProjectionService seatingMapProjectionService;
    private final CategoryProjectionDataService categoryProjectionService;
    private final SeatValidationService seatValidationService;

    /**
     * Secure M2M endpoint for the Scheduler Service to put a session on sale.
     * It is protected by requiring the 'internal-api' scope, which only the
     * scheduler-service-client's token will have.
     *
     * @param sessionId The ID of the session to put on sale.
     * @return A response entity indicating success.
     */
    @PatchMapping("sessions/{sessionId}/on-sale")
    public ResponseEntity<Void> putSessionOnSale(@PathVariable UUID sessionId) {
        eventLifecycleService.putSessionOnSale(sessionId);
        return ResponseEntity.ok().build();
    }

    /**
     * Secure M2M endpoint for the Scheduler Service to mark a session as SOLD_OUT.
     * This is called when a session ends or when all tickets are sold.
     *
     * @param sessionId The ID of the session to mark as SOLD_OUT.
     * @return A response entity indicating success.
     */
    @PatchMapping("sessions/{sessionId}/closed")
    public ResponseEntity<Void> markSessionAsClosed(@PathVariable UUID sessionId) {
        eventLifecycleService.markSessionAsClosed(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("events/{eventId}/projection-data")
    public ResponseEntity<EventProjectionDTO> getEventProjectionData(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventProjectionService.projectEvent(eventId));
    }

    @GetMapping("sessions/{sessionId}/projection-data")
    public ResponseEntity<SessionProjectionDTO> getSessionProjectionData(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionProjectionService.projectSession(sessionId));
    }

    @GetMapping("seating-maps/{seatingMapId}/projection-data")
    public ResponseEntity<SeatingMapProjectionDTO> getSeatingMapProjectionData(@PathVariable UUID seatingMapId) {
        return ResponseEntity.ok(seatingMapProjectionService.projectSeatingMap(seatingMapId));
    }

    /**
     * Endpoint for retrieving projection data for a specific category.
     * This includes details about the category and its associated seating map.
     *
     * @param categoryId The ID of the category to retrieve projection data for
     * @return The projection data for the specified category
     */
    @GetMapping("/categories/{categoryId}/projection-data")
    public ResponseEntity<CategoryProjectionDTO> getCategoryProjectionData(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(categoryProjectionService.getCategoryProjectionData(categoryId));
    }

    /**
     * Endpoint for validating and retrieving seat details for a specific session.
     * Returns seat information only if all seats are AVAILABLE, otherwise throws an error.
     * This is used by order service to validate seats before processing an order.
     *
     * @param sessionId The ID of the session the seats belong to
     * @param request   The request containing seat IDs to validate
     * @return List of seat details responses for all valid seats
     */
    @PostMapping("/sessions/{sessionId}/seats/details")
    public ResponseEntity<List<SeatDetailsResponse>> validateAndGetSeatsDetails(
            @RequestBody SeatDetailsRequest request, @PathVariable UUID sessionId) {
        List<SeatDetailsResponse> seatDetails = seatValidationService.validateAndGetSeatsDetails(sessionId, request);
        return ResponseEntity.ok(seatDetails);
    }
}
