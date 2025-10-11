package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.session.*;
import com.ticketly.mseventseating.service.session.SessionManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionManagementControllerTest {

    @Mock
    private SessionManagementService sessionManagementService;

    @InjectMocks
    private SessionManagementController controller;

    @Mock
    private Jwt jwt;

    private UUID sessionId;
    private UUID eventId;
    private String userId;
    private SessionResponse mockSessionResponse;
    private List<SessionResponse> mockSessionResponses;
    private SessionBatchResponse mockBatchResponse;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        userId = "test-user-id";
        
        // Setup mock JWT
        when(jwt.getClaimAsString("sub")).thenReturn(userId);

        // Setup mock response objects
        mockSessionResponse = SessionResponse.builder()
                .id(sessionId)
                .eventId(eventId)
                .build();
                
        mockSessionResponses = Arrays.asList(
            mockSessionResponse,
            SessionResponse.builder().id(UUID.randomUUID()).eventId(eventId).build()
        );
        
        mockBatchResponse = SessionBatchResponse.builder()
                .eventId(eventId)
                .totalCreated(1)
                .sessions(List.of(mockSessionResponse))
                .build();
    }

    @Test
    void createSessions_ShouldReturnCreatedResponse() {
        // Arrange
        CreateSessionsRequest request = new CreateSessionsRequest();
        request.setEventId(eventId);
        
        when(sessionManagementService.createSessions(eq(request), eq(userId), eq(jwt)))
                .thenReturn(mockBatchResponse);

        // Act
        ResponseEntity<SessionBatchResponse> response = controller.createSessions(request, jwt);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockBatchResponse, response.getBody());
        verify(sessionManagementService).createSessions(request, userId, jwt);
    }

    @Test
    void getSession_ShouldReturnSession() {
        // Arrange
        when(sessionManagementService.getSession(eq(sessionId), eq(userId)))
                .thenReturn(mockSessionResponse);

        // Act
        ResponseEntity<SessionResponse> response = controller.getSession(sessionId, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockSessionResponse, response.getBody());
        verify(sessionManagementService).getSession(sessionId, userId);
    }

    @Test
    void getSessionsByEventId_ShouldReturnSessionList() {
        // Arrange
        when(sessionManagementService.getSessionsByEvent(eq(eventId), eq(userId)))
                .thenReturn(mockSessionResponses);

        // Act
        ResponseEntity<List<SessionResponse>> response = controller.getSessionsByEventId(eventId, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockSessionResponses, response.getBody());
        assertEquals(2, response.getBody().size());
        verify(sessionManagementService).getSessionsByEvent(eventId, userId);
    }

    @Test
    void updateSessionTime_ShouldReturnUpdatedSession() {
        // Arrange
        SessionTimeUpdateDTO updateDTO = new SessionTimeUpdateDTO();
        
        when(sessionManagementService.updateSessionTime(eq(sessionId), eq(updateDTO), eq(userId)))
                .thenReturn(mockSessionResponse);

        // Act
        ResponseEntity<SessionResponse> response = controller.updateSessionTime(sessionId, updateDTO, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockSessionResponse, response.getBody());
        verify(sessionManagementService).updateSessionTime(sessionId, updateDTO, userId);
    }

    @Test
    void updateSessionStatus_ShouldReturnUpdatedSession() {
        // Arrange
        SessionStatusUpdateDTO updateDTO = new SessionStatusUpdateDTO();
        
        when(sessionManagementService.updateSessionStatus(eq(sessionId), eq(updateDTO), eq(userId)))
                .thenReturn(mockSessionResponse);

        // Act
        ResponseEntity<SessionResponse> response = controller.updateSessionStatus(sessionId, updateDTO, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockSessionResponse, response.getBody());
        verify(sessionManagementService).updateSessionStatus(sessionId, updateDTO, userId);
    }

    @Test
    void updateSessionVenue_ShouldReturnUpdatedSession() {
        // Arrange
        SessionVenueUpdateDTO updateDTO = new SessionVenueUpdateDTO();
        
        when(sessionManagementService.updateSessionVenue(eq(sessionId), eq(updateDTO), eq(userId)))
                .thenReturn(mockSessionResponse);

        // Act
        ResponseEntity<SessionResponse> response = controller.updateSessionVenue(sessionId, updateDTO, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockSessionResponse, response.getBody());
        verify(sessionManagementService).updateSessionVenue(sessionId, updateDTO, userId);
    }

    @Test
    void updateSessionVenueDetails_ShouldReturnUpdatedSession() {
        // Arrange
        SessionVenueDetailsUpdateDTO updateDTO = new SessionVenueDetailsUpdateDTO();
        
        when(sessionManagementService.updateSessionVenueDetails(eq(sessionId), eq(updateDTO), eq(userId)))
                .thenReturn(mockSessionResponse);

        // Act
        ResponseEntity<SessionResponse> response = controller.updateSessionVenueDetails(sessionId, updateDTO, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockSessionResponse, response.getBody());
        verify(sessionManagementService).updateSessionVenueDetails(sessionId, updateDTO, userId);
    }

    @Test
    void updateSessionLayout_ShouldReturnUpdatedSession() {
        // Arrange
        SessionLayoutUpdateDTO updateDTO = new SessionLayoutUpdateDTO();
        
        when(sessionManagementService.updateSessionLayout(eq(sessionId), eq(updateDTO), eq(userId)))
                .thenReturn(mockSessionResponse);

        // Act
        ResponseEntity<SessionResponse> response = controller.updateSessionLayout(sessionId, updateDTO, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockSessionResponse, response.getBody());
        verify(sessionManagementService).updateSessionLayout(sessionId, updateDTO, userId);
    }

    @Test
    void deleteSession_ShouldReturnNoContent() {
        // Arrange
        doNothing().when(sessionManagementService).deleteSession(eq(sessionId), eq(userId));

        // Act
        ResponseEntity<Void> response = controller.deleteSession(sessionId, jwt);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(sessionManagementService).deleteSession(sessionId, userId);
    }
}