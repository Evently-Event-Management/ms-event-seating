package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventOwnershipServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private EventOwnershipService eventOwnershipService;

    private UUID eventId;
    private String userId;
    private String nonOwnerId;
    private Event event;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        userId = "test-user-id";
        nonOwnerId = "non-owner-id";

        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setUserId(userId);
        organization.setName("Test Organization");

        event = new Event();
        event.setId(eventId);
        event.setOrganization(organization);
        event.setTitle("Test Event");
    }

    @Test
    @DisplayName("Should return true when user is owner")
    void isOwner_whenUserIsOwner_shouldReturnTrue() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        boolean result = eventOwnershipService.isOwner(eventId, userId);

        // Assert
        assertTrue(result);
        verify(eventRepository).findById(eventId);
    }

    @Test
    @DisplayName("Should return false when user is not owner")
    void isOwner_whenUserIsNotOwner_shouldReturnFalse() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        boolean result = eventOwnershipService.isOwner(eventId, nonOwnerId);

        // Assert
        assertFalse(result);
        verify(eventRepository).findById(eventId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when event not found")
    void isOwner_whenEventNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                eventOwnershipService.isOwner(eventId, userId));

        verify(eventRepository).findById(eventId);
    }

    @Test
    @DisplayName("Should evict event cache by ID")
    void evictEventCacheById_shouldDeleteCacheKeys() {
        // Arrange
        Set<String> mockKeys = Set.of("event-seating-ms::events::" + eventId + "-user1",
                "event-seating-ms::events::" + eventId + "-user2");
        when(redisTemplate.keys(anyString())).thenReturn(mockKeys);

        // Act
        eventOwnershipService.evictEventCacheById(eventId);

        // Assert
        verify(redisTemplate).keys("event-seating-ms::events::" + eventId + "-*");
        verify(redisTemplate).delete(mockKeys);
    }

    @Test
    @DisplayName("Should handle case when no cache keys found")
    void evictEventCacheById_whenNoKeysFound_shouldDoNothing() {
        // Arrange
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        // Act
        eventOwnershipService.evictEventCacheById(eventId);

        // Assert
        verify(redisTemplate).keys("event-seating-ms::events::" + eventId + "-*");
        verify(redisTemplate, never()).delete(any(Set.class));
    }
}
