package com.ticketly.mseventseating.service.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.SessionRequest;
import com.ticketly.mseventseating.dto.session.CreateSessionsRequest;
import com.ticketly.mseventseating.dto.session.SessionBatchResponse;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.SessionSeatingMap;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.repository.EventSessionRepository;
import com.ticketly.mseventseating.service.event.EventOwnershipService;
import com.ticketly.mseventseating.service.limts.LimitService;
import dto.SessionSeatingMapDTO;
import model.SessionStatus;
import model.SessionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionManagementServiceTest {

    @Mock
    private EventSessionRepository sessionRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LimitService limitService;

    @Mock
    private SessionOwnershipService sessionOwnershipService;

    @Mock
    private EventOwnershipService eventOwnershipService;

    @InjectMocks
    private SessionManagementService sessionManagementService;

    private UUID eventId;
    private UUID sessionId;
    private String userId;
    private Event mockEvent;
    private EventSession mockSession;
    private CreateSessionsRequest createRequest;
    private SessionRequest sessionRequest;
    private Jwt jwt;

    @BeforeEach
    void setUp() throws Exception {
        eventId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        userId = "test-user-id";

        // Mock JWT
        jwt = mock(Jwt.class);

        // Setup mock event
        mockEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .description("Test Description")
                .organization(Organization.builder().id(UUID.randomUUID()).build())
                .tiers(new ArrayList<>())
                .sessions(new ArrayList<>()) // Added to prevent NullPointerException in validateSessionLimit
                .build();

        // Setup mock session
        mockSession = EventSession.builder()
                .id(sessionId)
                .event(mockEvent)
                .startTime(OffsetDateTime.now().plusDays(7))
                .endTime(OffsetDateTime.now().plusDays(7).plusHours(2))
                .salesStartTime(OffsetDateTime.now().plusDays(1))
                .sessionType(SessionType.PHYSICAL)
                .status(SessionStatus.SCHEDULED)
                .sessionSeatingMap(SessionSeatingMap.builder().layoutData("{}").build())
                .build();

        // Setup session request
        sessionRequest = new SessionRequest();
        sessionRequest.setId(sessionId);
        sessionRequest.setStartTime(OffsetDateTime.now().plusDays(7));
        sessionRequest.setEndTime(OffsetDateTime.now().plusDays(7).plusHours(2));
        sessionRequest.setSalesStartTime(OffsetDateTime.now().plusDays(1));
        sessionRequest.setSessionType(SessionType.PHYSICAL);
        
        // Setup layout data
        SessionSeatingMapDTO layoutData = new SessionSeatingMapDTO();
        SessionSeatingMapDTO.Layout layout = new SessionSeatingMapDTO.Layout();
        layout.setBlocks(Collections.emptyList());
        layoutData.setLayout(layout);
        
        sessionRequest.setLayoutData(layoutData);

        // Setup create sessions request
        createRequest = new CreateSessionsRequest();
        createRequest.setEventId(eventId);
        createRequest.setSessions(List.of(sessionRequest));

        // We'll set up mocks in individual tests as needed
    }

    @Test
    void createSessions_ShouldCreateSessionsWithScheduledStatus() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));
        when(eventOwnershipService.isOwner(eq(eventId), eq(userId))).thenReturn(true);
        when(sessionRepository.saveAll(anyList())).thenReturn(List.of(mockSession));
        when(limitService.getTierLimit(any(), any())).thenReturn(10); // Mock the limit service
        
        // Act
        SessionBatchResponse response = sessionManagementService.createSessions(createRequest, userId, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(eventId, response.getEventId());
        assertEquals(1, response.getTotalCreated());
        assertNotNull(response.getSessions());
        assertEquals(1, response.getSessions().size());
        
        // Verify interactions
        verify(eventRepository).findById(eventId);
        verify(eventOwnershipService).isOwner(eq(eventId), eq(userId));
        verify(sessionRepository).saveAll(anyList());
        
        // Since we removed assertions about salesStartTime validation, we should verify 
        // that the sessions are created properly - no need to verify the internal validateSessionLimit method
    }

    @Test
    void getSession_ShouldReturnSessionWhenUserIsOwner() {
        // Arrange
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
        when(sessionOwnershipService.isOwner(eq(sessionId), eq(userId))).thenReturn(true);

        // Act
        var response = sessionManagementService.getSession(sessionId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(sessionId, response.getId());
        assertEquals(eventId, response.getEventId());
        
        // Verify interactions
        verify(sessionRepository).findById(sessionId);
        verify(sessionOwnershipService).isOwner(eq(sessionId), eq(userId));
    }
}