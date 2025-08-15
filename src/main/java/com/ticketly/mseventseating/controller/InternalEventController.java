package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.projection.EventProjectionDTO;
import com.ticketly.mseventseating.dto.projection.SeatingMapProjectionDTO;
import com.ticketly.mseventseating.dto.projection.SessionProjectionDTO;
import com.ticketly.mseventseating.service.event.EventLifecycleService;
import com.ticketly.mseventseating.service.event.EventProjectionService;
import com.ticketly.mseventseating.service.event.SeatingMapProjectionService;
import com.ticketly.mseventseating.service.event.SessionProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1") // A dedicated path for internal M2M calls
@RequiredArgsConstructor
public class InternalEventController {

    private final EventLifecycleService eventLifecycleService;
    private final EventProjectionService eventProjectionService;
    private final SessionProjectionService sessionProjectionService;
    private final SeatingMapProjectionService seatingMapProjectionService;

    /**
     * Secure M2M endpoint for the Scheduler Service to put a session on sale.
     * It is protected by requiring the 'internal-api' scope, which only the
     * scheduler-service-client's token will have.
     *
     * @param sessionId The ID of the session to put on sale.
     * @return A response entity indicating success.
     */
    @PatchMapping("sessions/{sessionId}/on-sale")
//    @PreAuthorize("hasAuthority('SCOPE_internal-api')") // ✅ The security check
    public ResponseEntity<Void> putSessionOnSale(@PathVariable UUID sessionId) {
        eventLifecycleService.putSessionOnSale(sessionId);
        return ResponseEntity.ok().build();
    }


    @GetMapping("events/{eventId}/projection-data")
//    @PreAuthorize("hasAuthority('SCOPE_internal-api')") // Secured by a specific scope
    public ResponseEntity<EventProjectionDTO> getEventProjectionData(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventProjectionService.projectEvent(eventId));
    }

    @GetMapping("sessions/{sessionId}/projection-data")
//    @PreAuthorize("hasAuthority('SCOPE_internal-api')") // Secured by
    public ResponseEntity<SessionProjectionDTO> getSessionProjectionData(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionProjectionService.projectSession(sessionId));
    }

    @GetMapping("seating-maps/{seatingMapId}/projection-data")
//    @PreAuthorize("hasAuthority('SCOPE_internal-api')") // Secured by
    public ResponseEntity<SeatingMapProjectionDTO> getSeatingMapProjectionData(@PathVariable UUID seatingMapId) {
        return ResponseEntity.ok(seatingMapProjectionService.projectSeatingMap(seatingMapId));
    }


}
