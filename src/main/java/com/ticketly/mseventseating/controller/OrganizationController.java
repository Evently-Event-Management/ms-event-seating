package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.OrganizationRequest;
import com.ticketly.mseventseating.dto.OrganizationResponse;
import com.ticketly.mseventseating.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Getting all organizations for user: {}", userId);
        return ResponseEntity.ok(organizationService.getAllOrganizationsForUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Getting organization with ID: {} for user: {}", id, userId);
        return ResponseEntity.ok(organizationService.getOrganizationById(id, userId));
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody OrganizationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Creating new organization for user: {}", userId);
        OrganizationResponse createdOrg = organizationService.createOrganization(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrg);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Updating organization with ID: {} for user: {}", id, userId);
        return ResponseEntity.ok(organizationService.updateOrganization(id, request, userId));
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrganizationResponse> uploadLogo(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) throws IOException {
        String userId = jwt.getSubject();
        log.info("Uploading logo for organization with ID: {} for user: {}", id, userId);
        return ResponseEntity.ok(organizationService.uploadLogo(id, file, userId));
    }

    @DeleteMapping("/{id}/logo")
    public ResponseEntity<Void> removeLogo(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Removing logo for organization with ID: {} for user: {}", id, userId);
        organizationService.removeLogo(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Deleting organization with ID: {} for user: {}", id, userId);
        organizationService.deleteOrganization(id, userId);
        return ResponseEntity.noContent().build();
    }
}
