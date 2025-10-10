package com.ticketly.mseventseating.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/public/health")
@Tag(name = "Health", description = "Health check endpoints")
public class HealthCheckController {

    @GetMapping
    @Operation(summary = "Check if the service is up", description = "Returns a 200 OK if the service is running")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service is up and running!");
    }
}