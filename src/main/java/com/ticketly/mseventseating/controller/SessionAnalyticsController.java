package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.session.OrganizationSessionDTO;
import com.ticketly.mseventseating.dto.session.SessionAnalyticsResponse;
import com.ticketly.mseventseating.service.session.SessionAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import model.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/session-analytics")
@RequiredArgsConstructor
@Tag(name = "Session Analytics", description = "API endpoints for retrieving session statistics and aggregated data")
public class SessionAnalyticsController {

    private final SessionAnalyticsService sessionAnalyticsService;

    /**
     * Get session counts grouped by status for a specific event
     * Owner endpoint - requires event ownership
     */
    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get session counts by status for an event",
            description = "Returns session counts grouped by status for a specific event. Requires organization owner permission.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized to access this event's data"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<SessionAnalyticsResponse> getEventSessionAnalytics(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal Jwt jwt) {

        SessionAnalyticsResponse response = sessionAnalyticsService.getSessionAnalyticsForEvent(eventId, jwt.getSubject());
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to get session counts by status for any event
     * Bypasses ownership verification as admins can view all events
     */
    @GetMapping("/admin/events/{eventId}")
    @PreAuthorize("hasRole('event_admin')")
    @Operation(summary = "Admin: Get session counts by status for any event",
            description = "Returns session counts grouped by status for any event. Admin access required.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized as admin"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<SessionAnalyticsResponse> getEventSessionAnalyticsAdmin(
            @PathVariable UUID eventId) {

        SessionAnalyticsResponse response = sessionAnalyticsService.getSessionAnalyticsForEvent(eventId, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Get session counts grouped by status for a specific organization
     * Owner endpoint - requires organization ownership
     */
    @GetMapping("/organizations/{organizationId}")
    @Operation(summary = "Get session counts by status for an organization",
            description = "Returns session counts grouped by status for a specific organization. Requires organization owner permission.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized to access this organization's data"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<SessionAnalyticsResponse> getOrganizationSessionAnalytics(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal Jwt jwt) {

        SessionAnalyticsResponse response = sessionAnalyticsService.getSessionAnalyticsForOrganization(organizationId, jwt.getSubject());
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to get session counts by status for any organization
     * Bypasses ownership verification as admins can view all organizations
     */
    @GetMapping("/admin/organizations/{organizationId}")
    @PreAuthorize("hasRole('event_admin')")
    @Operation(summary = "Admin: Get session counts by status for any organization",
            description = "Returns session counts grouped by status for any organization. Admin access required.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized as admin"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<SessionAnalyticsResponse> getOrganizationSessionAnalyticsAdmin(
            @PathVariable UUID organizationId) {

        SessionAnalyticsResponse response = sessionAnalyticsService.getSessionAnalyticsForOrganization(organizationId, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all sessions for a specific organization with filtering options
     * Owner endpoint - requires organization ownership
     */
    @GetMapping("/organizations/{organizationId}/sessions")
    @Operation(summary = "Get all sessions for an organization with filtering",
            description = "Returns all sessions for a specific organization with optional status filtering and sorting. " +
                    "Includes parent event details for each session. Requires organization owner permission.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized to access this organization's data"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<Page<OrganizationSessionDTO>> getOrganizationSessions(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) SessionStatus status,
            @PageableDefault(sort = {"salesStartTime", "startTime"}, direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        Page<OrganizationSessionDTO> response = sessionAnalyticsService.getOrganizationSessions(
                organizationId, jwt.getSubject(), status, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to get all sessions for any organization with filtering options
     * Bypasses ownership verification as admins can view all organizations
     */
    @GetMapping("/admin/organizations/{organizationId}/sessions")
    @PreAuthorize("hasRole('event_admin')")
    @Operation(summary = "Admin: Get all sessions for any organization with filtering",
            description = "Returns all sessions for any organization with optional status filtering and sorting. " +
                    "Includes parent event details for each session. Admin access required.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized as admin"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<Page<OrganizationSessionDTO>> getOrganizationSessionsAdmin(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) SessionStatus status,
            @PageableDefault(sort = {"salesStartTime", "startTime"}, direction = Sort.Direction.ASC) Pageable pageable) {

        Page<OrganizationSessionDTO> response = sessionAnalyticsService.getOrganizationSessions(
                organizationId, null, status, pageable
        );
        return ResponseEntity.ok(response);
    }
}