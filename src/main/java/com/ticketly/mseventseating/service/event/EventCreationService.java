package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.factory.EventFactory;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.OrganizationOwnershipService;
import com.ticketly.mseventseating.service.S3StorageService;
import com.ticketly.mseventseating.service.SubscriptionTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final SubscriptionTierService tierService;
    private final EventFactory eventFactory;
    private final S3StorageService s3StorageService; // ✅ Inject S3 service

    @Value("${app.event.max-cover-photos:5}")
    private int maxCoverPhotos;

    @Value("${app.event.max-cover-photo-size:31457280}")
    private long maxCoverPhotoSize;


    @Transactional
    public EventResponseDTO createEvent(CreateEventRequest request, MultipartFile[] coverImages, String userId, Jwt jwt) {
        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(request.getOrganizationId(), userId);
        validateTierLimits(organization.getId(), jwt, request.getSessions().size());

        // ✅ Step 1: Handle file uploads
        List<String> coverPhotoKeys = uploadCoverImages(coverImages);

        // ✅ Step 2: Pass the generated S3 keys to the factory
        Event event = eventFactory.createFromRequest(request, organization, coverPhotoKeys);

        Event savedEvent = eventRepository.save(event);
        log.info("Created new PENDING event with ID: {}", savedEvent.getId());

        // Note: Scheduling is NOT done here anymore. It's done upon APPROVAL.
        return mapToEventResponseDTO(savedEvent);
    }

    private void validateTierLimits(UUID organizationId, Jwt jwt, int numSessions) {
        int maxActiveEvents = tierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        long currentActiveEvents = eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
        if (currentActiveEvents >= maxActiveEvents) {
            throw new BadRequestException("You have reached the limit of " + maxActiveEvents + " active events for your tier.");
        }

        int maxSessions = tierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        if (numSessions > maxSessions) {
            throw new BadRequestException("You cannot create more than " + maxSessions + " sessions per event for your tier.");
        }
    }

    private List<String> uploadCoverImages(MultipartFile[] coverImages) {
        if (coverImages == null || coverImages.length == 0) {
            return new ArrayList<>(); // No images provided
        }

        if (coverImages.length > maxCoverPhotos) {
            throw new BadRequestException("You can upload a maximum of " + maxCoverPhotos + " cover photos.");
        }

        List<String> keys = new ArrayList<>();
        for (MultipartFile file : coverImages) {
            try {
                if (file.isEmpty() || !Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
                    throw new BadRequestException("Invalid file type detected. Please upload only image files.");
                }

                // Size verification
                if (file.getSize() > maxCoverPhotoSize) {
                    throw new BadRequestException("File size exceeds the maximum allowed size of " +
                            (maxCoverPhotoSize / (1024 * 1024)) + "MB");
                }

                String key = s3StorageService.uploadFile(file, "event-cover-photos");
                keys.add(key);
            } catch (IOException e) {
                log.error("Failed to upload cover image", e);
                // In a real system, you might want to delete already uploaded files if one fails
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