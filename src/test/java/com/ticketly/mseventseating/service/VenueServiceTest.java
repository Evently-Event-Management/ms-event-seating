package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.dto.venue.VenueRequest;
import com.ticketly.mseventseating.dto.venue.VenueResponse;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.Venue;
import com.ticketly.mseventseating.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    // The class we are testing
    @InjectMocks
    private VenueService venueService;

    // Mocking the dependencies
    @Mock
    private VenueRepository venueRepository;
    
    @Mock
    private OrganizationOwnershipService ownershipService;

    // Reusable test data
    private Organization organization;
    private Venue venue;
    private String userId;
    private String otherUserId;

    @BeforeEach
    void setUp() {
        // This method runs before each test to set up common objects
        userId = "user-auth-id-123";
        otherUserId = "other-user-id-456";

        organization = Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Organization")
                .userId(userId) // This user owns the organization
                .build();

        venue = Venue.builder()
                .id(UUID.randomUUID())
                .name("Test Venue")
                .address("123 Test St, Colombo")
                .capacity(500)
                .organization(organization) // Link venue to the organization
                .build();
    }

    // --- Tests for getVenueById ---

    @Test
    void getVenueById_WhenVenueExistsAndUserIsOwner_ShouldReturnVenueResponse() {
        // Arrange: Configure mocks to return our test data
        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), userId)).thenReturn(organization);

        // Act: Call the method being tested
        VenueResponse response = venueService.getVenueById(venue.getId(), userId);

        // Assert: Check that the result is correct
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(venue.getId());
        assertThat(response.getName()).isEqualTo(venue.getName());

        // Verify the ownership service was called
        verify(ownershipService).verifyOwnershipAndGetOrganization(organization.getId(), userId);
    }

    @Test
    void getVenueById_WhenVenueNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(venueRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> venueService.getVenueById(nonExistentId, userId));

        // Verify ownership service was never called
        verify(ownershipService, never()).verifyOwnershipAndGetOrganization(any(), any());
    }

    @Test
    void getVenueById_WhenUserIsNotOwner_ShouldThrowAuthorizationDeniedException() {
        // Arrange
        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), otherUserId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () -> {
            // Use a different user ID to simulate an unauthorized user
            venueService.getVenueById(venue.getId(), otherUserId);
        });

        // Verify the ownership service was called
        verify(ownershipService).verifyOwnershipAndGetOrganization(organization.getId(), otherUserId);
    }

    // --- Tests for createVenue ---

    @Test
    void createVenue_WhenUserIsOrganizationOwner_ShouldCreateAndReturnVenue() {
        // Arrange
        VenueRequest request = new VenueRequest();
        request.setName("New Awesome Venue");
        request.setAddress("456 New Rd, Galle");
        request.setCapacity(1000);
        request.setOrganizationId(organization.getId());

        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), userId)).thenReturn(organization);
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VenueResponse response = venueService.createVenue(request, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getOrganizationId()).isEqualTo(organization.getId());
        verify(venueRepository, times(1)).save(any(Venue.class)); // Verify save was called once
        verify(ownershipService).verifyOwnershipAndGetOrganization(organization.getId(), userId);
    }

    @Test
    void createVenue_WhenUserIsNotOrganizationOwner_ShouldThrowAuthorizationDeniedException() {
        // Arrange
        VenueRequest request = new VenueRequest();
        request.setOrganizationId(organization.getId());
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), otherUserId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () -> {
            // Use the "other" user ID
            venueService.createVenue(request, otherUserId);
        });
        
        // Verify the repository was never called
        verify(venueRepository, never()).save(any());
    }

    // --- Tests for updateVenue ---

    @Test
    void updateVenue_WhenUserIsOwner_ShouldUpdateAndReturnVenue() {
        // Arrange
        VenueRequest request = new VenueRequest();
        request.setName("Updated Venue Name");
        request.setAddress(venue.getAddress());
        request.setCapacity(venue.getCapacity());
        request.setOrganizationId(organization.getId());

        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), userId)).thenReturn(organization);
        when(venueRepository.save(any(Venue.class))).thenReturn(venue); // Return the updated venue

        // Act
        VenueResponse response = venueService.updateVenue(venue.getId(), request, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated Venue Name");
        verify(venueRepository, times(1)).save(venue);
        verify(ownershipService).verifyOwnershipAndGetOrganization(organization.getId(), userId);
    }

    @Test
    void updateVenue_WhenUserIsNotOwner_ShouldThrowAuthorizationDeniedException() {
        // Arrange
        VenueRequest request = new VenueRequest();
        request.setName("Updated Name");
        request.setOrganizationId(organization.getId());

        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), otherUserId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () -> venueService.updateVenue(venue.getId(), request, otherUserId));
        
        // Verify save was never called
        verify(venueRepository, never()).save(any());
    }

    @Test
    void updateVenue_WhenChangingToNewOrganization_ShouldVerifyOwnershipOfNewOrg() {
        // Arrange
        UUID newOrgId = UUID.randomUUID();
        Organization newOrg = Organization.builder()
                .id(newOrgId)
                .name("New Org")
                .userId(userId)
                .build();
                
        VenueRequest request = new VenueRequest();
        request.setName("Updated Name");
        request.setAddress(venue.getAddress());
        request.setOrganizationId(newOrgId);  // Different from current organization

        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), userId)).thenReturn(organization);
        when(ownershipService.verifyOwnershipAndGetOrganization(newOrgId, userId)).thenReturn(newOrg);
        when(venueRepository.save(any())).thenReturn(venue);

        // Act
        VenueResponse response = venueService.updateVenue(venue.getId(), request, userId);

        // Assert
        assertThat(response).isNotNull();
        verify(ownershipService).verifyOwnershipAndGetOrganization(organization.getId(), userId);
        verify(ownershipService).verifyOwnershipAndGetOrganization(newOrgId, userId);
    }

    @Test
    void updateVenue_WhenUserNotOwnerOfNewOrg_ShouldThrowAuthorizationDeniedException() {
        // Arrange
        UUID newOrgId = UUID.randomUUID();
        
        VenueRequest request = new VenueRequest();
        request.setName("Updated Name");
        request.setOrganizationId(newOrgId);  // Different from current organization

        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), userId)).thenReturn(organization);
        when(ownershipService.verifyOwnershipAndGetOrganization(newOrgId, userId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () -> venueService.updateVenue(venue.getId(), request, userId));
        
        // Verify proper methods were called
        verify(ownershipService).verifyOwnershipAndGetOrganization(organization.getId(), userId);
        verify(ownershipService).verifyOwnershipAndGetOrganization(newOrgId, userId);
        verify(venueRepository, never()).save(any());
    }

    // --- Tests for deleteVenue ---

    @Test
    void deleteVenue_WhenUserIsOwner_ShouldDeleteVenue() {
        // Arrange
        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), userId)).thenReturn(organization);
        // doNothing() is used for void methods
        doNothing().when(venueRepository).delete(venue);

        // Act
        venueService.deleteVenue(venue.getId(), userId);

        // Assert
        // Verify that the delete method was called exactly once with the correct venue object
        verify(venueRepository, times(1)).delete(venue);
        verify(ownershipService).verifyOwnershipAndGetOrganization(organization.getId(), userId);
    }

    @Test
    void deleteVenue_WhenUserIsNotOwner_ShouldThrowAuthorizationDeniedException() {
        // Arrange
        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), otherUserId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () -> venueService.deleteVenue(venue.getId(), otherUserId));

        // Verify delete was never called
        verify(venueRepository, never()).delete(any(Venue.class));
    }

    // --- Tests for getAllVenuesByOrganization ---

    @Test
    void getAllVenuesByOrganization_WhenUserIsOwner_ShouldReturnVenueList() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), userId)).thenReturn(organization);
        when(venueRepository.findByOrganizationId(organization.getId())).thenReturn(Collections.singletonList(venue));

        // Act
        List<VenueResponse> responses = venueService.getAllVenuesByOrganization(organization.getId(), userId);

        // Assert
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId()).isEqualTo(venue.getId());
        verify(ownershipService).verifyOwnershipAndGetOrganization(organization.getId(), userId);
    }

    @Test
    void getAllVenuesByOrganization_WhenUserIsNotOwner_ShouldThrowAuthorizationDeniedException() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(organization.getId(), otherUserId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () -> venueService.getAllVenuesByOrganization(organization.getId(), otherUserId));

        // Verify findByOrganizationId was never called
        verify(venueRepository, never()).findByOrganizationId(any());
    }
}
