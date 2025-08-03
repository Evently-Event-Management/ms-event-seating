package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.venue.VenueRequest;
import com.ticketly.mseventseating.dto.venue.VenueResponse;
import com.ticketly.mseventseating.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/venues")
@RequiredArgsConstructor
@Slf4j
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public ResponseEntity<List<VenueResponse>> getAllVenues(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Getting all venues for user: {}", userId);
        return ResponseEntity.ok(venueService.getAllVenuesForUser(userId));
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<VenueResponse>> getVenuesByOrganization(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Getting venues for organization ID: {} and user: {}", organizationId, userId);
        return ResponseEntity.ok(venueService.getAllVenuesByOrganization(organizationId, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueResponse> getVenueById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Getting venue with ID: {} for user: {}", id, userId);
        return ResponseEntity.ok(venueService.getVenueById(id, userId));
    }

    @PostMapping
    public ResponseEntity<VenueResponse> createVenue(
            @Valid @RequestBody VenueRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Creating new venue for user: {}", userId);
        VenueResponse createdVenue = venueService.createVenue(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVenue);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VenueResponse> updateVenue(
            @PathVariable UUID id,
            @Valid @RequestBody VenueRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Updating venue with ID: {} for user: {}", id, userId);
        return ResponseEntity.ok(venueService.updateVenue(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenue(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Deleting venue with ID: {} for user: {}", id, userId);
        venueService.deleteVenue(id, userId);
        return ResponseEntity.noContent().build();
    }
}
