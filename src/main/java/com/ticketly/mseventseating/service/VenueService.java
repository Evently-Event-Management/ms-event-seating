package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.dto.VenueRequest;
import com.ticketly.mseventseating.dto.VenueResponse;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.Venue;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import com.ticketly.mseventseating.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueService {

    private final VenueRepository venueRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Get all venues for a specific user
     *
     * @param userId the ID of the current user
     * @return List of venue responses
     */
    public List<VenueResponse> getAllVenuesForUser(String userId) {
        return venueRepository.findByOrganizationUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all venues for a specific organization
     *
     * @param organizationId the ID of the organization
     * @param userId the ID of the current user
     * @return List of venue responses
     */
    public List<VenueResponse> getAllVenuesByOrganization(UUID organizationId, String userId) {
        // Verify user has access to this organization
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        if (!organization.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("User does not have access to this organization");
        }

        return venueRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a venue by ID
     *
     * @param id the ID of the venue
     * @param userId the ID of the current user
     * @return the venue response
     */
    public VenueResponse getVenueById(UUID id, String userId) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + id));

        // Verify the user has access to the venue's organization
        if (!venue.getOrganization().getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("User does not have access to this venue");
        }

        return mapToDto(venue);
    }

    /**
     * Create a new venue
     *
     * @param request the venue request
     * @param userId the ID of the current user
     * @return the created venue response
     */
    @Transactional
    public VenueResponse createVenue(VenueRequest request, String userId) {
        // Get and verify organization access
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + request.getOrganizationId()));

        if (!organization.getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("User does not have access to this organization");
        }

        Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .capacity(request.getCapacity())
                .facilities(request.getFacilities())
                .organization(organization)
                .build();

        Venue savedVenue = venueRepository.save(venue);
        log.info("Venue created with ID: {}", savedVenue.getId());

        return mapToDto(savedVenue);
    }

    /**
     * Update an existing venue
     *
     * @param id the ID of the venue to update
     * @param request the venue request
     * @param userId the ID of the current user
     * @return the updated venue response
     */
    @Transactional
    public VenueResponse updateVenue(UUID id, VenueRequest request, String userId) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + id));

        // Verify user has access to the venue's organization
        if (!venue.getOrganization().getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("User does not have access to this venue");
        }

        // If organization ID is changed, verify user has access to the new organization
        if (!request.getOrganizationId().equals(venue.getOrganization().getId())) {
            Organization newOrganization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + request.getOrganizationId()));

            if (!newOrganization.getUserId().equals(userId)) {
                throw new AuthorizationDeniedException("User does not have access to the target organization");
            }

            venue.setOrganization(newOrganization);
        }

        venue.setName(request.getName());
        venue.setAddress(request.getAddress());
        venue.setLatitude(request.getLatitude());
        venue.setLongitude(request.getLongitude());
        venue.setCapacity(request.getCapacity());
        venue.setFacilities(request.getFacilities());

        Venue updatedVenue = venueRepository.save(venue);
        log.info("Venue updated with ID: {}", updatedVenue.getId());

        return mapToDto(updatedVenue);
    }

    /**
     * Delete a venue
     *
     * @param id the ID of the venue to delete
     * @param userId the ID of the current user
     */
    @Transactional
    public void deleteVenue(UUID id, String userId) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + id));

        // Verify user has access to the venue's organization
        if (!venue.getOrganization().getUserId().equals(userId)) {
            throw new AuthorizationDeniedException("User does not have access to this venue");
        }

        venueRepository.delete(venue);
        log.info("Venue deleted with ID: {}", id);
    }

    /**
     * Map a Venue entity to a VenueResponse DTO
     *
     * @param venue the venue entity
     * @return the venue response DTO
     */
    private VenueResponse mapToDto(Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .latitude(venue.getLatitude())
                .longitude(venue.getLongitude())
                .capacity(venue.getCapacity())
                .facilities(venue.getFacilities())
                .organizationId(venue.getOrganization().getId())
                .organizationName(venue.getOrganization().getName())
                .build();
    }
}
