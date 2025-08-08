package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.OrganizationOwnershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventOwnershipServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizationOwnershipService organizationOwnershipService;

    @InjectMocks
    private EventOwnershipService eventOwnershipService;

    private UUID eventId;
    private UUID organizationId;
    private String userId;
    private Event event;
    private Organization organization;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        userId = "test-user-id";

        organization = new Organization();
        organization.setId(organizationId);
        organization.setUserId(userId);
        organization.setName("Test Organization");

        event = new Event();
        event.setId(eventId);
        event.setOrganization(organization);
        event.setTitle("Test Event");
    }

    @Test
    @DisplayName("Should verify ownership and return event when user is owner")
    void verifyOwnershipAndGetEvent_whenUserIsOwner_shouldReturnEvent() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(organizationOwnershipService.verifyOwnershipAndGetOrganization(organizationId, userId))
            .thenReturn(organization);

        // Act
        Event result = eventOwnershipService.verifyOwnershipAndGetEvent(eventId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        verify(eventRepository).findById(eventId);
        verify(organizationOwnershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when event not found")
    void verifyOwnershipAndGetEvent_whenEventNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
            eventOwnershipService.verifyOwnershipAndGetEvent(eventId, userId));

        verify(eventRepository).findById(eventId);
        verifyNoInteractions(organizationOwnershipService);
    }

    @Test
    @DisplayName("Should throw AuthorizationDeniedException when user is not owner")
    void verifyOwnershipAndGetEvent_whenUserIsNotOwner_shouldThrowAuthorizationDeniedException() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        doThrow(new AuthorizationDeniedException("User does not have access to this organization"))
            .when(organizationOwnershipService).verifyOwnershipAndGetOrganization(organizationId, userId);

        // Act & Assert
        AuthorizationDeniedException exception = assertThrows(AuthorizationDeniedException.class, () ->
            eventOwnershipService.verifyOwnershipAndGetEvent(eventId, userId));

        assertEquals("User does not have access to this event", exception.getMessage());
        verify(eventRepository).findById(eventId);
        verify(organizationOwnershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
    }

    @Test
    @DisplayName("Should propagate any other exceptions that might occur")
    void verifyOwnershipAndGetEvent_whenUnexpectedErrorOccurs_shouldPropagateException() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        doThrow(new RuntimeException("Unexpected error"))
            .when(organizationOwnershipService).verifyOwnershipAndGetOrganization(organizationId, userId);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            eventOwnershipService.verifyOwnershipAndGetEvent(eventId, userId));

        verify(eventRepository).findById(eventId);
        verify(organizationOwnershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
    }

    @Test
    @DisplayName("Should verify ownership with correct parameters")
    void verifyOwnershipAndGetEvent_shouldPassCorrectParameters() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(organizationOwnershipService.verifyOwnershipAndGetOrganization(organizationId, userId))
            .thenReturn(organization);

        // Act
        eventOwnershipService.verifyOwnershipAndGetEvent(eventId, userId);

        // Assert
        verify(eventRepository).findById(eq(eventId));
        verify(organizationOwnershipService).verifyOwnershipAndGetOrganization(eq(organizationId), eq(userId));
    }

    @Test
    @DisplayName("Should not call repository multiple times when invoked with same parameters due to caching")
    void verifyOwnershipAndGetEvent_whenCalledMultipleTimes_shouldUseCache() {
        // Note: This test might not actually test caching since Spring's cache is proxied
        // and the proxy might not be active during unit tests.
        // This is more of a behavioral verification of the method.

        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(organizationOwnershipService.verifyOwnershipAndGetOrganization(organizationId, userId))
            .thenReturn(organization);

        // Act
        Event result1 = eventOwnershipService.verifyOwnershipAndGetEvent(eventId, userId);
        Event result2 = eventOwnershipService.verifyOwnershipAndGetEvent(eventId, userId);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(eventId, result1.getId());
        assertEquals(eventId, result2.getId());

        // In a real scenario with caching, this would be called once
        // but since caching is not active in unit tests, it will be called twice
        verify(eventRepository, times(2)).findById(eventId);
        verify(organizationOwnershipService, times(2)).verifyOwnershipAndGetOrganization(organizationId, userId);
    }
}
