package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.factory.EventFactory;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.LimitService;
import com.ticketly.mseventseating.service.OrganizationOwnershipService;
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
    private final OrganizationOwnershipService ownershipService;
    private final LimitService limitService;
    private final EventFactory eventFactory;
    private final S3StorageService s3StorageService;
    // private final EventSchedulingService eventSchedulingService; // This would be injected here

    @Transactional
    public EventResponseDTO createEvent(CreateEventRequest request, MultipartFile[] coverImages, String userId, Jwt jwt) {
        // 1. Authorization & Validation
        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(request.getOrganizationId(), userId);
        validateTierLimits(organization.getId(), jwt, request.getSessions().size());

        // 2. Handle File Uploads
        List<String> coverPhotoKeys = uploadCoverImages(coverImages);

        // 3. Build the Complete Object Graph in Memory
        Event event = eventFactory.createFromRequest(request, organization, coverPhotoKeys);

        // 4. Persist the Entire Aggregate in One Operation
        Event savedEvent = eventRepository.save(event);
        log.info("Created new PENDING event with ID: {}", savedEvent.getId());

        // 5. Orchestrate Side Effects (e.g., scheduling)
        // This is done after the event is successfully saved.
        // eventSchedulingService.scheduleOnSaleJobsForEvent(savedEvent);

        return mapToEventResponseDTO(savedEvent);
    }

    private void validateTierLimits(UUID organizationId, Jwt jwt, int numSessions) {
        int maxActiveEvents = limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        long currentActiveEvents = eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
        if (currentActiveEvents >= maxActiveEvents) {
            throw new BadRequestException("You have reached the limit of " + maxActiveEvents + " active events for your tier.");
        }

        int maxSessions = limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        if (numSessions > maxSessions) {
            throw new BadRequestException("You cannot create more than " + maxSessions + " sessions per event for your tier.");
        }
    }

    private List<String> uploadCoverImages(MultipartFile[] coverImages) {
        if (coverImages == null || coverImages.length == 0) {
            return new ArrayList<>();
        }

        int maxPhotos = limitService.getEventConfig().getMaxCoverPhotos();
        long maxSize = limitService.getEventConfig().getMaxCoverPhotoSize();

        if (coverImages.length > maxPhotos) {
            throw new BadRequestException("You can upload a maximum of " + maxPhotos + " cover photos.");
        }

        List<String> keys = new ArrayList<>();
        for (MultipartFile file : coverImages) {
            try {
                if (file.isEmpty() || !Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
                    throw new BadRequestException("Invalid file type detected. Please upload only image files.");
                }
                if (file.getSize() > maxSize) {
                    throw new BadRequestException("File size exceeds the maximum allowed size of " + (maxSize / (1024 * 1024)) + "MB");
                }
                String key = s3StorageService.uploadFile(file, "event-cover-photos");
                keys.add(key);
            } catch (IOException e) {
                log.error("Failed to upload cover image", e);
                throw new RuntimeException("Failed to upload cover image.", e);
            }
        }
        return keys;
    }

    private EventResponseDTO mapToEventResponseDTO(Event event) {
        return EventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .status(event.getStatus().name())
                .organizationId(event.getOrganization().getId())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
