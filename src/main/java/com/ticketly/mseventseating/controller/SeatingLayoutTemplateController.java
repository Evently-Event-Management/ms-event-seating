package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.SeatingLayoutTemplateDTO;
import com.ticketly.mseventseating.dto.SeatingLayoutTemplateRequest;
import com.ticketly.mseventseating.service.SeatingLayoutTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seating-templates")
@RequiredArgsConstructor
@Slf4j
public class SeatingLayoutTemplateController {

    private final SeatingLayoutTemplateService seatingLayoutTemplateService;

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<SeatingLayoutTemplateDTO>> getAllTemplatesByOrganization(
            @PathVariable UUID organizationId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Getting all templates for organization ID: {} by user: {}", organizationId, userId);
        return ResponseEntity.ok(seatingLayoutTemplateService.getAllTemplatesByOrganizationId(organizationId, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatingLayoutTemplateDTO> getTemplateById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Getting template with ID: {} by user: {}", id, userId);
        return ResponseEntity.ok(seatingLayoutTemplateService.getTemplateById(id, userId));
    }

    @PostMapping
    public ResponseEntity<SeatingLayoutTemplateDTO> createTemplate(
            @Valid @RequestBody SeatingLayoutTemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Creating new template for organization ID: {} by user: {}", 
                request.getOrganizationId(), userId);
        SeatingLayoutTemplateDTO created = seatingLayoutTemplateService.createTemplate(request, userId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeatingLayoutTemplateDTO> updateTemplate(
            @PathVariable UUID id, 
            @Valid @RequestBody SeatingLayoutTemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Updating template with ID: {} by user: {}", id, userId);
        return ResponseEntity.ok(seatingLayoutTemplateService.updateTemplate(id, request, userId));
    }

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
