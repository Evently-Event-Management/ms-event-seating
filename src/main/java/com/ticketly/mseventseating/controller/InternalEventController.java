package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.projection.EventProjectionDTO;
import com.ticketly.mseventseating.service.event.EventLifecycleService;
import com.ticketly.mseventseating.service.event.EventProjectionDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/sessions") // A dedicated path for internal M2M calls
@RequiredArgsConstructor
public class InternalEventController {

    private final EventLifecycleService eventLifecycleService;
    private final EventProjectionDataService projectionDataService;

    /**
     * Secure M2M endpoint for the Scheduler Service to put a session on sale.
     * It is protected by requiring the 'internal-api' scope, which only the
     * scheduler-service-client's token will have.
     *
     * @param sessionId The ID of the session to put on sale.
     * @return A response entity indicating success.
     */
    @PatchMapping("/{sessionId}/on-sale")
//    @PreAuthorize("hasAuthority('SCOPE_internal-api')") // ✅ The security check
    public ResponseEntity<Void> putSessionOnSale(@PathVariable UUID sessionId) {
        eventLifecycleService.putSessionOnSale(sessionId);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{eventId}/projection-data")
//    @PreAuthorize("hasAuthority('SCOPE_internal-api')") // Secured by a specific scope
    public ResponseEntity<EventProjectionDTO> getEventProjectionData(@PathVariable UUID eventId) {
        return ResponseEntity.ok(projectionDataService.getEventProjectionData(eventId));
    }
}
