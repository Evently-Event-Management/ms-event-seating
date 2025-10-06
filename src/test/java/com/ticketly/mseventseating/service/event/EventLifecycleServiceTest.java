package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.InvalidStateException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import model.EventStatus;
import model.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventLifecycleServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventOwnershipService eventOwnershipService;

    @InjectMocks
    private EventLifecycleService eventLifecycleService;

    private UUID eventId;
    private String userId;
    private Event event;
    private Organization organization;
    private EventSession pastSession;
    private EventSession futureSession;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        userId = "test-user-id";

        // Setup Organization
        organization = new Organization();
        organization.setId(organizationId);
        organization.setUserId("different-user-id"); // Different from test userId for approval tests
        organization.setName("Test Organization");

        // Setup Event
        event = new Event();
        event.setId(eventId);
        event.setTitle("Test Event");
        event.setDescription("Test Description");
        event.setOverview("Test Overview");
        event.setOrganization(organization);
        event.setStatus(EventStatus.PENDING);
        event.setTiers(new ArrayList<>());

        // Setup Past Session (more than an hour ago)
        pastSession = new EventSession();
        pastSession.setId(UUID.randomUUID());
        pastSession.setStartTime(OffsetDateTime.now().minusDays(1));
        pastSession.setEndTime(OffsetDateTime.now().minusDays(1).plusHours(2));
        pastSession.setStatus(SessionStatus.SCHEDULED);
        pastSession.setEvent(event);

        // Setup Future Session
        futureSession = new EventSession();
        futureSession.setId(UUID.randomUUID());
        futureSession.setStartTime(OffsetDateTime.now().plusDays(1));
        futureSession.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(2));
        futureSession.setStatus(SessionStatus.SCHEDULED);
        futureSession.setEvent(event);

        // Add sessions to event
        event.setSessions(new ArrayList<>(Collections.nCopies(2, null)));
        event.getSessions().set(0, pastSession);
        event.getSessions().set(1, futureSession);

        // Setup JWT with claims
        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .build();
    }

    @Test
    @DisplayName("Should approve event when user is not the owner and event is pending")
    void approveEvent_whenUserIsNotOwnerAndEventIsPending_shouldApproveEvent() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        eventLifecycleService.approveEvent(eventId, userId);

        // Assert
        assertEquals(EventStatus.APPROVED, event.getStatus());
        assertEquals(SessionStatus.CANCELLED, pastSession.getStatus());
        assertEquals(SessionStatus.SCHEDULED, futureSession.getStatus()); // Future session should be scheduled
        verify(eventRepository).findById(eventId);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("Should throw exception when user is the owner during approval")
    void approveEvent_whenUserIsOwner_shouldThrowException() {
        // Arrange
        organization.setUserId(userId); // Make the user the owner
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act & Assert
        AuthorizationDeniedException exception = assertThrows(AuthorizationDeniedException.class, () ->
                eventLifecycleService.approveEvent(eventId, userId));

        assertEquals("You cannot approve your own event.", exception.getMessage());
        verify(eventRepository).findById(eventId);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when approving non-pending event")
    void approveEvent_whenEventIsNotPending_shouldThrowException() {
        // Arrange
        event.setStatus(EventStatus.APPROVED); // Already approved
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act & Assert
        InvalidStateException exception = assertThrows(InvalidStateException.class, () ->
                eventLifecycleService.approveEvent(eventId, userId));

        assertEquals("Only events with PENDING status can be approved.", exception.getMessage());
        verify(eventRepository).findById(eventId);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when approving non-existent event")
    void approveEvent_whenEventDoesNotExist_shouldThrowException() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                eventLifecycleService.approveEvent(eventId, userId));

        assertEquals("Event not found with ID: " + eventId, exception.getMessage());
    }

    @Test
    @DisplayName("Should reject event when event is pending")
    void rejectEvent_whenEventIsPending_shouldRejectEvent() {
        // Arrange
        String rejectionReason = "Not suitable for our platform";
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        eventLifecycleService.rejectEvent(eventId, rejectionReason);

        // Assert
        assertEquals(EventStatus.REJECTED, event.getStatus());
        assertEquals(rejectionReason, event.getRejectionReason());
        verify(eventRepository).findById(eventId);
        verify(eventRepository).save(event);
    }

    @Test
    @DisplayName("Should throw exception when rejecting non-pending event")
    void rejectEvent_whenEventIsNotPending_shouldThrowException() {
        // Arrange
        event.setStatus(EventStatus.APPROVED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act & Assert
        InvalidStateException exception = assertThrows(InvalidStateException.class, () ->
                eventLifecycleService.rejectEvent(eventId, "Reason"));

        assertEquals("Only events with PENDING status can be rejected.", exception.getMessage());
        verify(eventRepository).findById(eventId);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete event when user is owner and event is pending")
    void deleteEvent_whenUserIsOwnerAndEventIsPending_shouldDeleteEvent() {
        // Arrange
        when(eventOwnershipService.isOwner(eventId, userId)).thenReturn(true);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        eventLifecycleService.deleteEvent(eventId, jwt.getSubject());

        // Assert
        verify(eventOwnershipService).isOwner(eventId, userId);
        verify(eventRepository).findById(eventId);
        verify(eventRepository).delete(event);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-pending event")
    void deleteEvent_whenEventIsNotPending_shouldThrowException() {
        // Arrange
        event.setStatus(EventStatus.APPROVED);
        when(eventOwnershipService.isOwner(eventId, userId)).thenReturn(true);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act & Assert
        InvalidStateException exception = assertThrows(InvalidStateException.class, () ->
                eventLifecycleService.deleteEvent(eventId, jwt.getSubject()));

        assertEquals("Only events with PENDING status can be deleted.", exception.getMessage());
        verify(eventOwnershipService).isOwner(eventId, userId);
        verify(eventRepository).findById(eventId);
        verify(eventRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when user is not owner during deletion")
    void deleteEvent_whenUserIsNotOwner_shouldThrowException() {
        // Arrange
        when(eventOwnershipService.isOwner(eventId, userId))
                .thenReturn(false);

        // Act & Assert
        AuthorizationDeniedException exception = assertThrows(AuthorizationDeniedException.class, () ->
                eventLifecycleService.deleteEvent(eventId, jwt.getSubject()));

        assertEquals("You are not authorized to delete this event.", exception.getMessage());
        verify(eventOwnershipService).isOwner(eventId, userId);
        // Never call findById or delete on the repository
        verify(eventRepository, never()).findById(any());
        verify(eventRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should handle past sessions correctly during approval")
    void approveEvent_withPastAndFutureSessions_shouldCancelPastSessions() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        eventLifecycleService.approveEvent(eventId, userId);

        // Assert
        assertEquals(EventStatus.APPROVED, event.getStatus());
        assertEquals(SessionStatus.CANCELLED, pastSession.getStatus());
        assertEquals(SessionStatus.SCHEDULED, futureSession.getStatus());
    }
}
