package com.ticketly.mseventseating.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for health check endpoints
 */
@RestController
public class HealthController {

    /**
     * Simple health check endpoint
     * @return A 200 OK response with basic health information
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", OffsetDateTime.now());
        healthInfo.put("service", "ms-event-seating");
        
        return ResponseEntity.ok(healthInfo);
    }
}