package com.ticketly.mseventseating.service.organization;


import com.ticketly.mseventseating.dto.organization.InviteStaffRequest;
import com.ticketly.mseventseating.dto.organization.OrganizationMemberResponse;
import com.ticketly.mseventseating.dto.organization.UpdateMemberStatusRequest;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.OrganizationMember;
import com.ticketly.mseventseating.repository.OrganizationMemberRepository;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import com.ticketly.mseventseating.service.session.SessionOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
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

    private final OrganizationOwnershipService organizationOwnershipService;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final Keycloak keycloakAdminClient;
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
        if (!organizationOwnershipService.isOwner(organizationId, ownerUserId)) {
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
        organizationOwnershipService.evictMemberRoleCacheByUser(UUID.fromString(user.getId()));
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
     * Gets a user's roles in an organization.
     *
     * @param organizationId the organization ID
     * @param userId         the user ID
     * @return the set of user roles or empty set if they have no roles
     */
    @Transactional(readOnly = true)
    public Optional<OrganizationMemberResponse> getMemberWithRoles(UUID organizationId, String userId, String requesterUserId) {

        // Verify the requester is either the organization owner or the user themselves
        if (!requesterUserId.equals(userId) && !organizationOwnershipService.isOwner(organizationId, requesterUserId)) {
            throw new AuthorizationDeniedException("Access denied to view member roles.");
        }


        // Check if user is the organization member
        Optional<OrganizationMember> member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId);

        return member.map(organizationMember -> OrganizationMemberResponse.builder()
                .userId(organizationMember.getUserId())
                .roles(organizationMember.getRoles())
                .build());
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
    public void removeStaff(UUID organizationId, String userIdToRemove, String ownerUserId) {
        // Verify the person removing the staff owns the organization
        if (!organizationOwnershipService.isOwner(organizationId, ownerUserId)) {
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
    public List<OrganizationMemberResponse> getAllOrganizationMembers(UUID organizationId, String ownerUserId) {
        // Verify the person is the organization owner or a member with appropriate role
        if (!organizationOwnershipService.isOwner(organizationId, ownerUserId)) {
            throw new AuthorizationDeniedException("Only the organization owner can view all staff members.");
        }

        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        // Get all members and convert to response DTOs
        return memberRepository.findByOrganizationId(organizationId).stream()
                .map(member -> {
                    // Fetch user by ID directly instead of searching by email
                    UserRepresentation user = null;
                    try {
                        user = keycloakAdminClient.realm(realm).users().get(member.getUserId()).toRepresentation();
                        log.debug("Found Keycloak user: {} for member ID: {}", user.getUsername(), member.getUserId());
                    } catch (Exception e) {
                        log.warn("Could not fetch Keycloak user with ID: {}. Error: {}", member.getUserId(), e.getMessage());
                    }

                    return OrganizationMemberResponse.builder()
                            .userId(member.getUserId())
                            .email(user != null ? user.getEmail() : "Unknown")
                            .name(user != null ? (user.getFirstName() + " " + user.getLastName()) : "Unknown")
                            .roles(member.getRoles())
                            .isActive(member.isActive())
                            .build();
                })
                .toList();
    }

    /**
     * Updates the active status of a staff member in an organization.
     *
     * @param organizationId the organization ID
     * @param memberId the member's user ID
     * @param request the request containing the new active status
     * @param ownerUserId the user ID of the requester (must be organization owner)
     * @return the updated organization member response
     */
    @Transactional
    @CacheEvict(value = {"organizationMemberRoles"}, allEntries = true)
    public OrganizationMemberResponse updateMemberStatus(UUID organizationId, String memberId,
                                                        UpdateMemberStatusRequest request, String ownerUserId) {
        // Verify the person updating the member status owns the organization
        if (!organizationOwnershipService.isOwner(organizationId, ownerUserId)) {
            throw new AuthorizationDeniedException("Only the organization owner can update staff status.");
        }

        // Find the member
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in organization: " + memberId));

        // Update the active status
        member.setActive(request.isActive());
        memberRepository.save(member);

        // Evict cache for this specific member
        organizationOwnershipService.evictMemberRoleCacheByUser(UUID.fromString(memberId));

        log.info("Updated active status for user {} in organization {} to {}",
                memberId, organizationId, request.isActive());

        // Fetch user information from Keycloak to include in the response
        UserRepresentation user = null;
        try {
            user = keycloakAdminClient.realm(realm).users().get(memberId).toRepresentation();
            log.debug("Found Keycloak user: {} for member ID: {}", user.getUsername(), memberId);
        } catch (Exception e) {
            log.warn("Could not fetch Keycloak user with ID: {}. Error: {}", memberId, e.getMessage());
        }

        // Convert to response
        return new OrganizationMemberResponse(
                member.getId(),
                member.getUserId(),
                user != null ? user.getEmail() : "Unknown",
                user != null ? (user.getFirstName() + " " + user.getLastName()) : "Unknown",
                member.getRoles(),
                member.isActive()
        );
    }
}
