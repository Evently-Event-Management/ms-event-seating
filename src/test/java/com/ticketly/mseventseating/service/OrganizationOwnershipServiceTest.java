package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationOwnershipServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationOwnershipService ownershipService;

    @Test
    void isOrganizationOwnedByUser_ShouldReturnTrue_WhenUserIsOwner() {
        // Arrange
        String userId = "owner-user-id";
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder().id(orgId).userId(userId).build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        // Act
        boolean result = ownershipService.isOrganizationOwnedByUser(userId, orgId);

        // Assert
        assertTrue(result);
        verify(organizationRepository, times(1)).findById(orgId);
    }

    @Test
    void isOrganizationOwnedByUser_ShouldReturnFalse_WhenUserIsNotOwner() {
        // Arrange
        String ownerId = "owner-user-id";
        String requesterId = "different-user-id";
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder().id(orgId).userId(ownerId).build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        // Act
        boolean result = ownershipService.isOrganizationOwnedByUser(requesterId, orgId);

        // Assert
        assertFalse(result);
        verify(organizationRepository, times(1)).findById(orgId);
    }

    @Test
    void isOrganizationOwnedByUser_ShouldReturnFalse_WhenOrganizationNotFound() {
        // Arrange
        String userId = "any-user-id";
        UUID orgId = UUID.randomUUID();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        // Act
        boolean result = ownershipService.isOrganizationOwnedByUser(userId, orgId);

        // Assert
        assertFalse(result);
        verify(organizationRepository, times(1)).findById(orgId);
    }
}
