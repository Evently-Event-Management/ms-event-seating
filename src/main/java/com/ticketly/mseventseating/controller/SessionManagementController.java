package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.session.CreateSessionsRequest;
import com.ticketly.mseventseating.dto.session.SessionBatchResponse;
import com.ticketly.mseventseating.dto.session.SessionResponse;
import com.ticketly.mseventseating.dto.session.SessionTimeUpdateDTO;
import com.ticketly.mseventseating.service.session.SessionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Session Management", description = "API endpoints for managing event sessions")
public class SessionManagementController {

    private final SessionManagementService sessionManagementService;

    /**
     * Create multiple sessions for an event
     */
    @PostMapping
    @Operation(summary = "Create multiple sessions for an event", 
              description = "Creates multiple sessions for a specific event. Requires organization owner permission.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Sessions created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
        @ApiResponse(responseCode = "403", description = "User not authorized to create sessions"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<SessionBatchResponse> createSessions(
            @Valid @RequestBody CreateSessionsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getClaimAsString("sub");
        SessionBatchResponse response = sessionManagementService.createSessions(request, userId, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a session by ID
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get a session by ID", 
              description = "Retrieves a specific session by its ID. Organization owners and scanners can access.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "User not authorized to access this session"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<SessionResponse> getSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getClaimAsString("sub");
        SessionResponse response = sessionManagementService.getSession(sessionId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a session
     */
    @PutMapping("/{sessionId}")
    @Operation(summary = "Update a session", 
              description = "Updates an existing session. Only allowed before sales start. Only organization owners can update.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request, validation error, or session not editable"),
        @ApiResponse(responseCode = "403", description = "User not authorized to update this session"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<SessionResponse> updateSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SessionTimeUpdateDTO updateDTO,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getClaimAsString("sub");
        SessionResponse response = sessionManagementService.updateSession(sessionId, updateDTO, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a session
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete a session", 
              description = "Deletes an existing session. Only allowed before sales start. Only organization owners can delete.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Session deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Session cannot be deleted (sales started or session ended)"),
        @ApiResponse(responseCode = "403", description = "User not authorized to delete this session"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getClaimAsString("sub");
        sessionManagementService.deleteSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }
}