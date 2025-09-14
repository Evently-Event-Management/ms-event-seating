package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.layout_template.SeatingLayoutTemplateDTO;
import com.ticketly.mseventseating.dto.layout_template.SeatingLayoutTemplateRequest;
import com.ticketly.mseventseating.service.seating_layout.SeatingLayoutTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seating-templates")
@RequiredArgsConstructor
@Slf4j
public class SeatingLayoutTemplateController {

    private final SeatingLayoutTemplateService seatingLayoutTemplateService;

    /**
     * Get all seating layout templates for a specific organization, paginated.
     *
     * @param organizationId The ID of the organization.
     * @param page The page number to retrieve.
     * @param size The number of templates per page.
     * @param jwt The authenticated user's JWT.
     * @return A paginated list of seating layout templates.
     */
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<Page<SeatingLayoutTemplateDTO>> getAllTemplatesByOrganization(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Getting paginated templates for organization ID: {} by user: {} (page: {}, size: {})",
                organizationId, userId, page, size);
        return ResponseEntity.ok(seatingLayoutTemplateService.getAllTemplatesByOrganizationId(organizationId, userId, page, size));
    }

    /**
     * Get a specific seating layout template by its ID.
     *
     * @param id The template ID.
     * @param jwt The authenticated user's JWT.
     * @return The seating layout template.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SeatingLayoutTemplateDTO> getTemplateById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Getting template with ID: {} by user: {}", id, userId);
        return ResponseEntity.ok(seatingLayoutTemplateService.getTemplateById(id, userId));
    }

    /**
     * Create a new seating layout template for an organization.
     *
     * @param request The template creation request.
     * @param jwt The authenticated user's JWT.
     * @return The created seating layout template.
     */
    @PostMapping
    public ResponseEntity<SeatingLayoutTemplateDTO> createTemplate(
            @Valid @RequestBody SeatingLayoutTemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Creating new template for organization ID: {} by user: {}",
                request.getOrganizationId(), userId);
        SeatingLayoutTemplateDTO created = seatingLayoutTemplateService.createTemplate(request, userId, jwt);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update an existing seating layout template.
     *
     * @param id The template ID.
     * @param request The update request.
     * @param jwt The authenticated user's JWT.
     * @return The updated seating layout template.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SeatingLayoutTemplateDTO> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody SeatingLayoutTemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Updating template with ID: {} by user: {}", id, userId);
        return ResponseEntity.ok(seatingLayoutTemplateService.updateTemplate(id, request, userId));
    }

    /**
     * Delete a seating layout template by its ID.
     *
     * @param id The template ID.
     * @param jwt The authenticated user's JWT.
     * @return No content response.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Deleting template with ID: {} by user: {}", id, userId);
        seatingLayoutTemplateService.deleteTemplate(id, userId);
        return ResponseEntity.noContent().build();
    }
}