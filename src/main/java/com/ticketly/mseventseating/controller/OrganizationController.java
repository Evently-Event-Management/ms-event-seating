package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.organization.InviteStaffRequest;
import com.ticketly.mseventseating.dto.organization.OrganizationMemberResponse;
import com.ticketly.mseventseating.dto.organization.OrganizationRequest;
import com.ticketly.mseventseating.dto.organization.OrganizationResponse;
import com.ticketly.mseventseating.dto.organization.UpdateMemberStatusRequest;
import com.ticketly.mseventseating.service.organization.OrganizationService;
import com.ticketly.mseventseating.service.organization.OrganizationStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationStaffService organizationStaffService;

    /**
     * Get all organizations owned by the authenticated user.
     */
    @GetMapping("/my")
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizationsByOwner(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Fetching organizations owned by user: {}", userId);
        return ResponseEntity.ok(organizationService.getAllOrganizationsForUser(userId));
    }

    /**
     * Admin: Get all organizations for a specific user.
     */
    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasRole('organization_admin')")
    public ResponseEntity<List<OrganizationResponse>> getOrganizationsByUserId(
            @PathVariable String userId) {
        log.info("Admin fetching organizations for user: {}", userId);
        return ResponseEntity.ok(organizationService.getAllOrganizationsForUser(userId));
    }

    /**
     * Get organization details by ID for the owner.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("User {} requesting details for organization ID: {}", userId, id);
        return ResponseEntity.ok(organizationService.getOrganizationByIdOwner(id, userId));
    }

    /**
     * Admin: Get organization details by ID.
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('organization_admin')")
    public ResponseEntity<OrganizationResponse> getOrganizationByIdAdmin(
            @PathVariable UUID id) {
        log.info("Admin requesting details for organization ID: {}", id);
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    /**
     * Create a new organization for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody OrganizationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("User {} creating a new organization", userId);
        OrganizationResponse createdOrg = organizationService.createOrganization(request, userId, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrg);
    }

    /**
     * Update organization details for the owner.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("User {} updating organization ID: {}", userId, id);
        return ResponseEntity.ok(organizationService.updateOrganization(id, request, userId));
    }

    /**
     * Upload a logo for the organization.
     */
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrganizationResponse> uploadLogo(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) throws IOException {
        String userId = jwt.getSubject();
        log.info("User {} uploading logo for organization ID: {}", userId, id);
        return ResponseEntity.ok(organizationService.uploadLogo(id, file, userId));
    }

    /**
     * Remove the logo from the organization.
     */
    @DeleteMapping("/{id}/logo")
    public ResponseEntity<Void> removeLogo(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("User {} removing logo for organization ID: {}", userId, id);
        organizationService.removeLogo(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete the organization for the owner.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("User {} deleting organization ID: {}", userId, id);
        organizationService.deleteOrganization(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all staff members of an organization.
     */
    @GetMapping("/{organizationId}/staff")
    public ResponseEntity<List<OrganizationMemberResponse>> getAllStaffMembers(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal Jwt jwt) {
        String ownerUserId = jwt.getSubject();
        log.info("User {} fetching all staff members for organization {}", ownerUserId, organizationId);
        List<OrganizationMemberResponse> members = organizationStaffService.getAllOrganizationMembers(organizationId, ownerUserId);
        return ResponseEntity.ok(members);
    }

    /**
     * Invite a new staff member to an organization or update an existing member's roles.
     */
    @PostMapping("/{organizationId}/staff")
    public ResponseEntity<OrganizationMemberResponse> inviteStaff(
            @PathVariable UUID organizationId,
            @Valid @RequestBody InviteStaffRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String ownerUserId = jwt.getSubject();
        log.info("User {} inviting staff member with email {} to organization {}", ownerUserId, request.getEmail(), organizationId);
        OrganizationMemberResponse newMember = organizationStaffService.inviteStaff(organizationId, request, ownerUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(newMember);
    }

    /**
     * Remove a staff member from an organization.
     */
    @DeleteMapping("/{organizationId}/staff/{userId}")
    public ResponseEntity<Void> removeStaff(
            @PathVariable UUID organizationId,
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {
        String ownerUserId = jwt.getSubject();
        log.info("User {} removing staff member {} from organization {}", ownerUserId, userId, organizationId);
        organizationStaffService.removeStaff(organizationId, userId, ownerUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update the active status of a staff member in an organization.
     */
    @PatchMapping("/{organizationId}/staff/{userId}/status")
    public ResponseEntity<OrganizationMemberResponse> updateStaffStatus(
            @PathVariable UUID organizationId,
            @PathVariable String userId,
            @Valid @RequestBody UpdateMemberStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String ownerUserId = jwt.getSubject();
        log.info("User {} updating status of staff member {} in organization {} to active: {}",
                ownerUserId, userId, organizationId, request.isActive());
        OrganizationMemberResponse updatedMember = organizationStaffService.updateMemberStatus(
                organizationId, userId, request, ownerUserId);
        return ResponseEntity.ok(updatedMember);
    }
}
