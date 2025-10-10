package com.ticketly.mseventseating.controller;


import com.ticketly.mseventseating.dto.tier.CreateTierRequest;
import com.ticketly.mseventseating.dto.tier.TierResponseDTO;
import com.ticketly.mseventseating.dto.tier.UpdateTierRequest;
import com.ticketly.mseventseating.service.tier.TierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/events/{eventId}/tiers")
@RequiredArgsConstructor
@Tag(name = "Tier Management", description = "APIs for managing event tiers")
public class TierController {
    private final TierService tierService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new tier", description = "Creates a new tier for the specified event")
    public TierResponseDTO createTier(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateTierRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        return tierService.createTier(eventId, request, userId);
    }

    @PutMapping("/{tierId}")
    @Operation(summary = "Update a tier", description = "Updates an existing tier for the specified event")
    public TierResponseDTO updateTier(
            @PathVariable UUID eventId,
            @PathVariable UUID tierId,
            @Valid @RequestBody UpdateTierRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        return tierService.updateTier(eventId, tierId, request, userId);
    }
}