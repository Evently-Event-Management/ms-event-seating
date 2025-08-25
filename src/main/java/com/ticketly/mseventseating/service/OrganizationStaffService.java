package com.ticketly.mseventseating.service;


import com.ticketly.mseventseating.dto.organization.InviteStaffRequest;
import com.ticketly.mseventseating.dto.organization.OrganizationMemberResponse;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.OrganizationMember;
import com.ticketly.mseventseating.model.OrganizationRole;
import com.ticketly.mseventseating.repository.OrganizationMemberRepository;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import com.ticketly.mseventseating.service.event.EventOwnershipService;
import com.ticketly.mseventseating.service.event.SessionOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationStaffService {

    private final OrganizationOwnershipService ownershipService;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;
    private final Keycloak keycloakAdminClient;
    private final EventOwnershipService eventOwnershipService;
    private final SessionOwnershipService sessionOwnershipService;

    @Value("${keycloak.realm:event-ticketing}")
    private String realm;

    /**
     * Invites a staff member to an organization with the specified roles.
     * If the member already exists, their roles will be updated.
     *
     * @param organizationId the organization ID
     * @param request        the invitation request with set of roles
     * @param ownerUserId    the user ID of the inviter
     * @return the created or updated organization member
     */
    @Transactional
    @CacheEvict(value = {"organizationMemberRoles"}, allEntries = true)
    public OrganizationMemberResponse inviteStaff(UUID organizationId, InviteStaffRequest request, String ownerUserId) {
        // 1. Verify the person sending the invite owns the organization
        if (!ownershipService.isOwner(organizationId, ownerUserId)) {
            throw new AuthorizationDeniedException("Only the organization owner can invite staff.");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        // 2. Find or create the user in Keycloak
        List<UserRepresentation> users = keycloakAdminClient.realm(realm).users().searchByEmail(request.getEmail(), true);
        UserRepresentation user;

        if (users.isEmpty()) {
            // User does not exist, create them
            user = new UserRepresentation();
            user.setEmail(request.getEmail());
            user.setUsername(request.getEmail());
            user.setEnabled(true); // User is enabled but must set password
            keycloakAdminClient.realm(realm).users().create(user);

            // Re-fetch the user to get their new ID
            user = keycloakAdminClient.realm(realm).users().searchByEmail(request.getEmail(), true).getFirst();

            // Send the "Execute Actions Email" to prompt them to set a password
            keycloakAdminClient.realm(realm).users().get(user.getId()).executeActionsEmail(List.of("UPDATE_PASSWORD"));
            log.info("Created new user in Keycloak with email: {}", request.getEmail());
        } else {
            user = users.getFirst();
        }

        // 3. Check if the user is already a member of the organization
        Optional<OrganizationMember> existingMember = memberRepository.findByOrganizationIdAndUserId(organizationId, user.getId());
        OrganizationMember member;

        if (existingMember.isPresent()) {
            // Update the existing member's roles
            member = existingMember.get();
            member.setRoles(request.getRoles());
            log.info("Updated roles for user {} in organization {} to {}", user.getId(), organizationId, request.getRoles());
        } else {
            // Create a new member
            member = OrganizationMember.builder()
                    .organization(organization)
                    .userId(user.getId())
                    .roles(request.getRoles())
                    .build();
            log.info("Added user {} to organization {} with roles {}", user.getId(), organizationId, request.getRoles());
        }

        memberRepository.save(member);

        // Evict related caches for this user
        ownershipService.evictOrganizationCacheByUser(UUID.fromString(user.getId()));
        eventOwnershipService.evictEventCacheByUser(user.getId());
        sessionOwnershipService.evictSessionCacheByUser(user.getId());

        // 4. Return the response
        return OrganizationMemberResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getFirstName() + " " + user.getLastName())
                .roles(request.getRoles())
                .build();
    }

    /**
     * Checks if a user has a specific role in an organization.
     *
     * @param organizationId the organization ID
     * @param userId         the user ID
     * @param role           the role to check for
     * @return true if the user has the specified role
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "organizationMemberRoles", key = "#organizationId + '-' + #userId + '-' + #role")
    public boolean hasRole(UUID organizationId, String userId, OrganizationRole role) {
        log.info("--- DATABASE HIT: Checking if user {} has role {} in organization {} ---", userId, role, organizationId);

        // First check if user is the owner (owners have all privileges)
        if (ownershipService.isOwner(organizationId, userId)) {
            return true;
        }

        // Then check for specific role in the set of roles
        Optional<OrganizationMember> member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId);
        return member.isPresent() && member.get().getRoles().contains(role);
    }

    /**
     * Gets a user's roles in an organization.
     *
     * @param organizationId the organization ID
     * @param userId         the user ID
     * @return the set of user roles or empty set if they have no roles
     */
    @Transactional(readOnly = true)
    public Optional<OrganizationMemberResponse> getMemberWithRoles(UUID organizationId, String userId) {
        log.info("--- DATABASE HIT: Fetching roles for user {} in organization {} ---", userId, organizationId);

        // Check if user is the organization member
        Optional<OrganizationMember> member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId);

        return member.map(organizationMember -> OrganizationMemberResponse.builder()
                .userId(organizationMember.getUserId())
                .roles(organizationMember.getRoles())
                .build());

        // Convert to response DTO
        // For a full implementation, you'd fetch user details from Keycloak
    }

    /**
     * Removes a staff member from an organization.
     *
     * @param organizationId the organization ID
     * @param userIdToRemove the ID of the user to remove
     * @param ownerUserId    the ID of the organization owner making the request
     * @throws AuthorizationDeniedException if the requester is not the organization owner
     * @throws ResourceNotFoundException    if the organization or member is not found
     */
    @Transactional
    @CacheEvict(value = {"organizationMemberRoles", "organizationMemberDetails"}, allEntries = true)
    public void removeStaff(UUID organizationId, String userIdToRemove, String ownerUserId) {
        // Verify the person removing the staff owns the organization
        if (!ownershipService.isOwner(organizationId, ownerUserId)) {
            throw new AuthorizationDeniedException("Only the organization owner can remove staff members.");
        }

        // Verify the organization exists
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }

        // Find the member to remove
        Optional<OrganizationMember> member = memberRepository.findByOrganizationIdAndUserId(organizationId, userIdToRemove);

        if (member.isEmpty()) {
            throw new ResourceNotFoundException("Staff member not found in organization.");
        }

        // Remove the member
        memberRepository.delete(member.get());
        log.info("Removed user {} from organization {}", userIdToRemove, organizationId);

        // Evict related caches for this user
        ownershipService.evictOrganizationCacheByUser(UUID.fromString(userIdToRemove));
        eventOwnershipService.evictEventCacheByUser(userIdToRemove);
        sessionOwnershipService.evictSessionCacheByUser(userIdToRemove);
    }

    /**
     * Gets all members of an organization.
     *
     * @param organizationId the organization ID
     * @param ownerUserId    the ID of the user making the request
     * @return a list of all organization members
     * @throws AuthorizationDeniedException if the requester is not the organization owner
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "organizationAllMembers", key = "#organizationId")
    public List<OrganizationMemberResponse> getAllOrganizationMembers(UUID organizationId, String ownerUserId) {
        log.info("--- DATABASE HIT: Fetching all members for organization {} ---", organizationId);

        // Verify the person is the organization owner or a member with appropriate role
        if (!ownershipService.isOwner(organizationId, ownerUserId)) {
            throw new AuthorizationDeniedException("Only the organization owner can view all staff members.");
        }

        // Get the organization to verify it exists
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        // Get all members and convert to response DTOs
        return memberRepository.findByOrganizationId(organizationId).stream()
                .map(member -> {
                    // Ideally fetch user details from Keycloak for each member
                    List<UserRepresentation> users = keycloakAdminClient.realm(realm).users().search(null, null, null, member.getUserId(), 0, 1);
                    UserRepresentation user = users.isEmpty() ? null : users.getFirst();

                    return OrganizationMemberResponse.builder()
                            .userId(member.getUserId())
                            .email(user != null ? user.getEmail() : "Unknown")
                            .name(user != null ? (user.getFirstName() + " " + user.getLastName()) : "Unknown")
                            .roles(member.getRoles())
                            .build();
                })
                .toList();
    }
}
