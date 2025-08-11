package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.repository.OrganizationRepository;
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
class OrganizationOwnershipServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private OrganizationOwnershipService ownershipService;

    private UUID organizationId;
    private String ownerId;
    private String nonOwnerId;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        ownerId = "owner-user-id";
        nonOwnerId = "non-owner-id";

        organization = Organization.builder()
                .id(organizationId)
                .userId(ownerId)
                .name("Test Organization")
                .build();
    }

    @Test
    @DisplayName("Should return true when user is owner")
    void isOwner_whenUserIsOwner_shouldReturnTrue() {
        // Arrange
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        // Act
        boolean result = ownershipService.isOwner(organizationId, ownerId);

        // Assert
        assertTrue(result);
        verify(organizationRepository).findById(organizationId);
    }

    @Test
    @DisplayName("Should return false when user is not owner")
    void isOwner_whenUserIsNotOwner_shouldReturnFalse() {
        // Arrange
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

        // Act
        boolean result = ownershipService.isOwner(organizationId, nonOwnerId);

        // Assert
        assertFalse(result);
        verify(organizationRepository).findById(organizationId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when organization not found")
    void isOwner_whenOrganizationNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
            ownershipService.isOwner(organizationId, ownerId));

        verify(organizationRepository).findById(organizationId);
    }

    @Test
    @DisplayName("Should evict organization cache by ID")
    void evictOrganizationCacheById_shouldDeleteCacheKeys() {
        // Arrange
        Set<String> mockKeys = Set.of(
                "event-seating-ms::organizationOwnership::" + organizationId + "-user1",
                "event-seating-ms::organizationOwnership::" + organizationId + "-user2");
        when(redisTemplate.keys(anyString())).thenReturn(mockKeys);

        // Act
        ownershipService.evictOrganizationCacheById(organizationId);

        // Assert
        verify(redisTemplate).keys("event-seating-ms::organizationOwnership::" + organizationId + "-*");
        verify(redisTemplate).delete(mockKeys);
    }

    @Test
    @DisplayName("Should handle case when no cache keys found")
    void evictOrganizationCacheById_whenNoKeysFound_shouldDoNothing() {
        // Arrange
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        // Act
        ownershipService.evictOrganizationCacheById(organizationId);

        // Assert
        verify(redisTemplate).keys("event-seating-ms::organizationOwnership::" + organizationId + "-*");
        verify(redisTemplate, never()).delete(any(Set.class));
    }
}
