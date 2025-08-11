package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.service.event.EventLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/sessions") // A dedicated path for internal M2M calls
@RequiredArgsConstructor
public class InternalEventController {

    private final EventLifecycleService eventLifecycleService;

    /**
     * Secure M2M endpoint for the Scheduler Service to put a session on sale.
     * It is protected by requiring the 'internal-api' scope, which only the
     * scheduler-service-client's token will have.
     *
     * @param sessionId The ID of the session to put on sale.
     * @return A response entity indicating success.
     */
    @PatchMapping("/{sessionId}/on-sale")
    @PreAuthorize("hasAuthority('SCOPE_internal-api')") // ✅ The security check
    public ResponseEntity<Void> putSessionOnSale(@PathVariable UUID sessionId) {
        eventLifecycleService.putSessionOnSale(sessionId);
        return ResponseEntity.ok().build();
    }
}
