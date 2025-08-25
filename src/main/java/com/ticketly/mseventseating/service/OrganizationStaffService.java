package com.ticketly.mseventseating.service;


import com.ticketly.mseventseating.dto.organization.InviteStaffRequest;
import com.ticketly.mseventseating.dto.organization.OrganizationMemberResponse;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.OrganizationMember;
import com.ticketly.mseventseating.repository.OrganizationMemberRepository;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationStaffService {

    private final OrganizationOwnershipService ownershipService;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.realm:event-ticketing}")
    private String realm;

    @Transactional
    public OrganizationMemberResponse inviteStaff(UUID organizationId, InviteStaffRequest request, String ownerUserId) {
        // 1. Verify the person sending the invite owns the organization
//        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(organizationId, ownerUserId);
        if (!ownershipService.isOwner(organizationId, ownerUserId)) {
            throw new AuthorizationDeniedException("Only the organization owner can invite staff.");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + organizationId));


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

        // 3. Save the relationship in your local database
        OrganizationMember newMember = OrganizationMember.builder()
                .organization(organization)
                .userId(user.getId())
                .role(request.getRole())
                .build();

        memberRepository.save(newMember);
        log.info("Added user {} to organization {} with role {}", user.getId(), organizationId, request.getRole());

        // 4. Return the response
        return OrganizationMemberResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getFirstName() + " " + user.getLastName())
                .role(request.getRole())
                .build();
    }
}
