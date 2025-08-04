package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.AppConfigDTO;
import com.ticketly.mseventseating.service.LimitService; // ✅ Updated import
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/config")
@RequiredArgsConstructor
public class LimitConfigurationController {

    private final LimitService limitService; // ✅ Use the new unified service

    /**
     * Public endpoint to fetch all tier-based and general application limits.
     */
    @GetMapping
    public ResponseEntity<AppConfigDTO> getAppConfiguration() {
        return ResponseEntity.ok(limitService.getAppConfiguration());
    }
}
