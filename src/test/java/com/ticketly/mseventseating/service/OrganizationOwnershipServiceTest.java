package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;

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
    void verifyOwnershipAndGetOrganization_ShouldReturnOrganization_WhenUserIsOwner() {
        // Arrange
        String userId = "owner-user-id";
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder().id(orgId).userId(userId).build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        // Act
        Organization result = ownershipService.verifyOwnershipAndGetOrganization(orgId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(orgId, result.getId());
        assertEquals(userId, result.getUserId());
        verify(organizationRepository, times(1)).findById(orgId);
    }

    @Test
    void verifyOwnershipAndGetOrganization_ShouldThrowAuthorizationDeniedException_WhenUserIsNotOwner() {
        // Arrange
        String ownerId = "owner-user-id";
        String requesterId = "different-user-id";
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder().id(orgId).userId(ownerId).build();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () -> ownershipService.verifyOwnershipAndGetOrganization(orgId, requesterId));

        verify(organizationRepository, times(1)).findById(orgId);
    }

    @Test
    void verifyOwnershipAndGetOrganization_ShouldThrowResourceNotFoundException_WhenOrganizationNotFound() {
        // Arrange
        String userId = "any-user-id";
        UUID orgId = UUID.randomUUID();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> ownershipService.verifyOwnershipAndGetOrganization(orgId, userId));

        verify(organizationRepository, times(1)).findById(orgId);
    }
}
