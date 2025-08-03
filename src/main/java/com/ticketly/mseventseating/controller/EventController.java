package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.service.event.EventCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventCreationService eventCreationService;

    @PostMapping
    // Any authenticated user can create an event, ownership is checked in the service
    public ResponseEntity<EventResponseDTO> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        EventResponseDTO createdEvent = eventCreationService.createEvent(request, userId, jwt);
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }

    // You would add endpoints here for an organizer to GET their own events,
    // UPDATE their PENDING events, etc.
}
