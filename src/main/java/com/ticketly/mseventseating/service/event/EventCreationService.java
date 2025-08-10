package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.factory.EventFactory;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.LimitService;
import com.ticketly.mseventseating.service.OrganizationService;
import com.ticketly.mseventseating.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCreationService {

    private final EventRepository eventRepository;
    private final LimitService limitService;
    private final EventFactory eventFactory;
    private final S3StorageService s3StorageService;
    private final OrganizationService organizationService;
    // private final EventSchedulingService eventSchedulingService; // This would be injected here

    @Transactional
    public EventResponseDTO createEvent(CreateEventRequest request, MultipartFile[] coverImages, String userId, Jwt jwt) {
        log.info("Creating new event for organization: {} by user: {}", request.getOrganizationId(), userId);
        log.debug("Event creation request: title='{}', sessions={}, tiers={}",
                request.getTitle(), request.getSessions().size(), request.getTiers().size());

        // 1. Authorization & Validation
        log.debug("Verifying organization ownership for organization ID: {}", request.getOrganizationId());
        Organization organization = organizationService.verifyOwnershipAndGetOrganization(request.getOrganizationId(), userId);

        log.debug("Validating tier limits for organization: {}", organization.getId());
        validateTierLimits(organization.getId(), jwt, request.getSessions().size());

        // 2. Handle File Uploads
        log.debug("Processing {} cover images", coverImages != null ? coverImages.length : 0);
        List<String> coverPhotoKeys = uploadCoverImages(coverImages);
        log.debug("Successfully uploaded {} cover photos", coverPhotoKeys.size());

        // 3. Build the Complete Object Graph in Memory
        log.debug("Building event from request");
        Event event = eventFactory.createFromRequest(request, organization, coverPhotoKeys);

        // 4. Persist the Entire Aggregate in One Operation
        log.debug("Saving event to database");
        Event savedEvent = eventRepository.save(event);
        log.info("Created new PENDING event with ID: {} and title: '{}'", savedEvent.getId(), savedEvent.getTitle());

        // 5. Orchestrate Side Effects (e.g., scheduling)
        // This is done after the event is successfully saved.
        // log.debug("Scheduling on-sale jobs for event: {}", savedEvent.getId());
        // eventSchedulingService.scheduleOnSaleJobsForEvent(savedEvent);

        return mapToEventResponseDTO(savedEvent);
    }

    private void validateTierLimits(UUID organizationId, Jwt jwt, int numSessions) {
        log.debug("Validating tier limits for organization: {}, sessions: {}", organizationId, numSessions);

        int maxActiveEvents = limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        long currentActiveEvents = eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
        log.debug("Current active events: {}, max allowed: {}", currentActiveEvents, maxActiveEvents);

        if (currentActiveEvents >= maxActiveEvents) {
            log.warn("Organization {} has reached active event limit: {}", organizationId, maxActiveEvents);
            throw new BadRequestException("You have reached the limit of " + maxActiveEvents + " active events for your tier.");
        }

        int maxSessions = limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        log.debug("Max sessions per event for tier: {}", maxSessions);
        if (numSessions > maxSessions) {
            log.warn("Request exceeds session limit: requested={}, max={}", numSessions, maxSessions);
            throw new BadRequestException("You cannot create more than " + maxSessions + " sessions per event for your tier.");
        }

        log.debug("Tier limits validation successful");
    }

    private List<String> uploadCoverImages(MultipartFile[] coverImages) {
        if (coverImages == null || coverImages.length == 0) {
            log.debug("No cover images to upload");
            return new ArrayList<>();
        }

        int maxPhotos = limitService.getEventConfig().getMaxCoverPhotos();
        long maxSize = limitService.getEventConfig().getMaxCoverPhotoSize();
        log.debug("Cover photo limits: maxPhotos={}, maxSize={}MB", maxPhotos, maxSize / (1024 * 1024));

        if (coverImages.length > maxPhotos) {
            log.warn("Too many cover photos: {}, max allowed: {}", coverImages.length, maxPhotos);
            throw new BadRequestException("You can upload a maximum of " + maxPhotos + " cover photos.");
        }

        List<String> keys = new ArrayList<>();
        for (int i = 0; i < coverImages.length; i++) {
            MultipartFile file = coverImages[i];
            try {
                log.debug("Processing cover photo {}/{}: size={}KB, contentType={}",
                        i + 1, coverImages.length, file.getSize() / 1024, file.getContentType());

                if (file.isEmpty() || !Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
                    log.warn("Invalid file type detected: {}", file.getContentType());
                    throw new BadRequestException("Invalid file type detected. Please upload only image files.");
                }
                if (file.getSize() > maxSize) {
                    log.warn("File size too large: {}KB, max: {}KB", file.getSize() / 1024, maxSize / 1024);
                    throw new BadRequestException("File size exceeds the maximum allowed size of " + (maxSize / (1024 * 1024)) + "MB");
                }
                String key = s3StorageService.uploadFile(file, "event-cover-photos");
                log.debug("Successfully uploaded image to S3, key: {}", key);
                keys.add(key);
            } catch (IOException e) {
                log.error("Failed to upload cover image {}/{}", i + 1, coverImages.length, e);
                throw new RuntimeException("Failed to upload cover image.", e);
            }
        }
        log.debug("Successfully uploaded {} cover photos", keys.size());
        return keys;
    }

    private EventResponseDTO mapToEventResponseDTO(Event event) {
        log.debug("Mapping event {} to response DTO", event.getId());
        return EventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .status(event.getStatus().name())
                .organizationId(event.getOrganization().getId())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
