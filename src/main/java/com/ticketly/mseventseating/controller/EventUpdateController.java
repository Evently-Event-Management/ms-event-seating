package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.dto.event.UpdateEventRequest;
import com.ticketly.mseventseating.service.event.EventUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/v1/events/{eventId}/update")
@RequiredArgsConstructor
@Slf4j
public class EventUpdateController {

    private final EventUpdateService eventUpdateService;
    
    /**
     * Update basic event details (title, description, overview)
     */
    @PutMapping
    public ResponseEntity<EventResponseDTO> updateEventDetails(
            @PathVariable UUID eventId,
            @RequestBody @Valid UpdateEventRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Updating event details for event: {}", eventId);
        String userId = jwt.getSubject();
        EventResponseDTO updatedEvent = eventUpdateService.updateEventDetails(eventId, request, userId);
        return ResponseEntity.ok(updatedEvent);
    }
    
    /**
     * Add a cover photo to an event
     */
    @PostMapping(path = "/cover-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventResponseDTO> addCoverPhoto(
            @PathVariable UUID eventId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Adding cover photo to event: {}", eventId);
        String userId = jwt.getSubject();
        
        EventResponseDTO updatedEvent = eventUpdateService.addCoverPhoto(eventId, file, userId);
        return ResponseEntity.ok(updatedEvent);
    }
    
    /**
     * Remove a cover photo from an event
     */
    @DeleteMapping("/cover-photos/{photoId}")
    public ResponseEntity<EventResponseDTO> removeCoverPhoto(
            @PathVariable UUID eventId,
            @PathVariable UUID photoId,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Removing cover photo: {} from event: {}", photoId, eventId);
        String userId = jwt.getSubject();
        
        EventResponseDTO updatedEvent = eventUpdateService.removeCoverPhoto(eventId, photoId, userId);
        return ResponseEntity.ok(updatedEvent);
    }
}