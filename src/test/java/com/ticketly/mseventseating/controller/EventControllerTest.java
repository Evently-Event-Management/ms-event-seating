package com.ticketly.mseventseating.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.*;
import com.ticketly.mseventseating.service.event.EventCreationService;
import com.ticketly.mseventseating.service.event.EventLifecycleService;
import com.ticketly.mseventseating.service.event.EventQueryService;
import model.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventControllerTest {

    @Mock
    private EventCreationService eventCreationService;

    @Mock
    private EventLifecycleService eventLifecycleService;

    @Mock
    private EventQueryService eventQueryService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventController eventController;

    private UUID eventId;
    private UUID organizationId;
    private String userId;
    private Jwt jwt;
    private CreateEventRequest createEventRequest;
    private EventResponseDTO eventResponseDTO;
    private EventDetailDTO eventDetailDTO;
    private Page<EventSummaryDTO> eventSummaryPage;
    private RejectEventRequest rejectEventRequest;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        userId = "user-123";

        // Create mock JWT with subject (user ID)
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), headers, claims);

        // Set up a valid create event request
        createEventRequest = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Event Description")
                .overview("Test Event Overview")
                .organizationId(organizationId)
                .venueId(venueId)
                .categoryId(categoryId)
                .tiers(Collections.singletonList(new TierRequest()))
                .sessions(Collections.singletonList(new SessionRequest()))
                .build();

        // Set up event response DTO
        eventResponseDTO = EventResponseDTO.builder()
                .id(eventId)
                .title("Test Event")
                .status(EventStatus.PENDING.name())
                .organizationId(organizationId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Set up event detail DTO
        eventDetailDTO = EventDetailDTO.builder()
                .id(eventId)
                .title("Test Event")
                .description("Test Event Description")
                .overview("Test Event Overview")
                .status(EventStatus.PENDING)
                .organizationId(organizationId)
                .organizationName("Test Organization")
                .categoryId(categoryId)
                .categoryName("Test Category")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .tiers(new ArrayList<>())
                .build();

        // Set up event summary DTO
        EventSummaryDTO eventSummary = EventSummaryDTO.builder()
                .id(eventId)
                .title("Test Event")
                .status(EventStatus.PENDING)
                .organizationId(organizationId)
                .organizationName("Test Organization")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        List<EventSummaryDTO> eventSummaryList = Collections.singletonList(eventSummary);
        eventSummaryPage = new PageImpl<>(eventSummaryList, PageRequest.of(0, 10), 1);

        // Set up reject event request
        rejectEventRequest = RejectEventRequest.builder()
                .reason("Test rejection reason")
                .build();
    }

    @Test
    void createEvent_ShouldReturnCreatedEvent() throws IOException {
        // Arrange
        String requestStr = "{\"title\":\"Test Event\"}";
        MultipartFile[] coverImages = new MultipartFile[]{
                new MockMultipartFile("coverImage", "test.jpg", "image/jpeg", "test".getBytes())
        };

        when(objectMapper.readValue(requestStr, CreateEventRequest.class)).thenReturn(createEventRequest);
        when(eventCreationService.createEvent(eq(createEventRequest), eq(coverImages), eq(userId), any(Jwt.class)))
                .thenReturn(eventResponseDTO);

        // Act
        ResponseEntity<EventResponseDTO> response = eventController.createEvent(requestStr, coverImages, jwt);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(eventResponseDTO, response.getBody());
        verify(eventCreationService).createEvent(eq(createEventRequest), eq(coverImages), eq(userId), any(Jwt.class));
    }

    @Test
    void getEventDetailsOwner_ShouldReturnEventDetails() {
        // Arrange
        when(eventQueryService.findEventByIdOwner(eventId, userId)).thenReturn(eventDetailDTO);

        // Act
        ResponseEntity<EventDetailDTO> response = eventController.getEventDetailsOwner(eventId, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(eventDetailDTO, response.getBody());
        verify(eventQueryService).findEventByIdOwner(eventId, userId);
    }

    @Test
    void getEventDetails_ShouldReturnEventDetails() {
        // Arrange
        when(eventQueryService.findEventById(eventId)).thenReturn(eventDetailDTO);

        // Act
        ResponseEntity<EventDetailDTO> response = eventController.getEventDetails(eventId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(eventDetailDTO, response.getBody());
        verify(eventQueryService).findEventById(eventId);
    }

    @Test
    void deleteEvent_ShouldReturnNoContent() {
        // Arrange
        doNothing().when(eventLifecycleService).deleteEvent(eventId, userId);

        // Act
        ResponseEntity<Void> response = eventController.deleteEvent(eventId, jwt);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(eventLifecycleService).deleteEvent(eventId, userId);
    }

    @Test
    void getOrganizationEventsOwner_ShouldReturnEvents() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        EventStatus status = EventStatus.PENDING;
        String search = "test";

        when(eventQueryService.findEventsByOrganizationOwner(
                organizationId, userId, status, search, pageable))
                .thenReturn(eventSummaryPage);

        // Act
        ResponseEntity<Page<EventSummaryDTO>> response = eventController.getOrganizationEventsOwner(
                organizationId, status, search, pageable, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(eventSummaryPage, response.getBody());
        verify(eventQueryService).findEventsByOrganizationOwner(organizationId, userId, status, search, pageable);
    }

    @Test
    void getOrganizationEvents_ShouldReturnEvents() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        EventStatus status = EventStatus.PENDING;
        String search = "test";

        when(eventQueryService.findEventsByOrganization(organizationId, status, search, pageable))
                .thenReturn(eventSummaryPage);

        // Act
        ResponseEntity<Page<EventSummaryDTO>> response = eventController.getOrganizationEvents(
                organizationId, status, search, pageable);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(eventSummaryPage, response.getBody());
        verify(eventQueryService).findEventsByOrganization(organizationId, status, search, pageable);
    }

    @Test
    void getAllEvents_ShouldReturnAllEvents() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        EventStatus status = EventStatus.PENDING;
        String search = "test";

        when(eventQueryService.findAllEvents(status, search, pageable)).thenReturn(eventSummaryPage);

        // Act
        ResponseEntity<Page<EventSummaryDTO>> response = eventController.getAllEvents(status, search, pageable);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(eventSummaryPage, response.getBody());
        verify(eventQueryService).findAllEvents(status, search, pageable);
    }

    @Test
    void approveEvent_ShouldReturnOk() {
        // Arrange
        doNothing().when(eventLifecycleService).approveEvent(eventId, userId);

        // Act
        ResponseEntity<Void> response = eventController.approveEvent(eventId, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventLifecycleService).approveEvent(eventId, userId);
    }

    @Test
    void rejectEvent_ShouldReturnOk() {
        // Arrange
        doNothing().when(eventLifecycleService).rejectEvent(eventId, rejectEventRequest.getReason());

        // Act
        ResponseEntity<Void> response = eventController.rejectEvent(eventId, rejectEventRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventLifecycleService).rejectEvent(eventId, rejectEventRequest.getReason());
    }
}
