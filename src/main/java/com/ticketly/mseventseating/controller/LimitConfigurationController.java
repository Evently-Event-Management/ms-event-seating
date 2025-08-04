package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.config.AppConfigDTO;
import com.ticketly.mseventseating.dto.config.MyLimitsResponseDTO;
import com.ticketly.mseventseating.service.LimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/config")
@RequiredArgsConstructor
public class LimitConfigurationController {

    private final LimitService limitService;

    /**
     * Public endpoint to fetch all tier-based and general application limits.
     * Useful for displaying upgrade options or pricing pages.
     */
    @GetMapping
    public ResponseEntity<AppConfigDTO> getAppConfiguration() {
        return ResponseEntity.ok(limitService.getAppConfiguration());
    }

    /**
     * Authenticated endpoint to fetch the limits and configuration
     * relevant to the current user's subscription tier.
     */
    @GetMapping("/my-limits")
    public ResponseEntity<MyLimitsResponseDTO> getMyLimits(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(limitService.getMyLimits(jwt));
    }
}
