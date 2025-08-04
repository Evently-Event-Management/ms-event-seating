package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.event.RejectEventRequest;
import com.ticketly.mseventseating.service.event.EventLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/events") // A dedicated path for admin actions
@RequiredArgsConstructor
public class AdminEventController {

    private final EventLifecycleService eventLifecycleService;

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('APPROVE_EVENT')") // Secured by a specific Keycloak role
    public ResponseEntity<Void> approveEvent(@PathVariable UUID id,
                                             @AuthenticationPrincipal Jwt jwt) {
        eventLifecycleService.approveEvent(id, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('APPROVE_EVENT')")
    public ResponseEntity<Void> rejectEvent(
            @PathVariable UUID id,
            @Valid @RequestBody RejectEventRequest request) {
        eventLifecycleService.rejectEvent(id, request.getReason());
        return ResponseEntity.ok().build();
    }
}