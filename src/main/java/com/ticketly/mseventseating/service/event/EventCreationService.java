package com.ticketly.mseventseating.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.factory.EventFactory;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.LimitService;
import com.ticketly.mseventseating.service.OrganizationOwnershipService;
import com.ticketly.mseventseating.service.S3StorageService;
import com.ticketly.mseventseating.dto.event.TierRequest;
import com.ticketly.mseventseating.dto.event.SessionSeatingMapRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCreationService {

    private final EventRepository eventRepository;
    private final OrganizationOwnershipService ownershipService;
    private final LimitService limitService; // ✅ Inject the new LimitService
    private final EventFactory eventFactory;
    private final S3StorageService s3StorageService; // ✅ Inject S3 service
    private final ObjectMapper objectMapper; // For JSON serialization/deserialization

    private int getMaxCoverPhotos() {
        return limitService.getEventConfig().getMaxCoverPhotos();
    }

    private long getMaxCoverPhotoSize() {
        return limitService.getEventConfig().getMaxCoverPhotoSize();
    }

    @Transactional
    public EventResponseDTO createEvent(CreateEventRequest request, MultipartFile[] coverImages, String userId, Jwt jwt) {
        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(request.getOrganizationId(), userId);
        validateTierLimits(organization.getId(), jwt, request.getSessions().size());

        // ✅ Step 1: Handle file uploads
        List<String> coverPhotoKeys = uploadCoverImages(coverImages);

        // ✅ Step 2: Pass the generated S3 keys to the factory
        Event event = eventFactory.createFromRequest(request, organization, coverPhotoKeys);

        // Save the event to generate IDs for all entities
        Event savedEvent = eventRepository.save(event);

        // ✅ Step 3: Update tier IDs in session seating maps
        updateTierIdsInSeatingMaps(savedEvent, request.getTiers());

        log.info("Created new PENDING event with ID: {}", savedEvent.getId());

        // Note: Scheduling is NOT done here anymore. It's done upon APPROVAL.
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
            return new ArrayList<>(); // No images provided
        }

        if (coverImages.length > getMaxCoverPhotos()) {
            throw new BadRequestException("You can upload a maximum of " + getMaxCoverPhotos() + " cover photos.");
        }

        List<String> keys = new ArrayList<>();
        for (MultipartFile file : coverImages) {
            try {
                if (file.isEmpty() || !Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
                    throw new BadRequestException("Invalid file type detected. Please upload only image files.");
                }

                // Size verification
                if (file.getSize() > getMaxCoverPhotoSize()) {
                    throw new BadRequestException("File size exceeds the maximum allowed size of " +
                            (getMaxCoverPhotoSize() / (1024 * 1024)) + "MB");
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

    /**
     * Updates the tier IDs in the seating maps after the event and tiers have been saved.
     * This replaces temporary tier IDs with actual database IDs.
     */
    private void updateTierIdsInSeatingMaps(Event event, List<TierRequest> tierRequests) {
        // Create mapping from request tier IDs to actual persisted tier IDs
        Map<String, UUID> tierIdMapping = new HashMap<>();
        for (int i = 0; i < tierRequests.size(); i++) {
            String requestTierId = tierRequests.get(i).getId();
            UUID actualTierId = event.getTiers().get(i).getId();
            tierIdMapping.put(requestTierId, actualTierId);
        }

        // Update each session's seating map
        for (EventSession session : event.getSessions()) {
            try {
                SessionSeatingMap map = session.getSessionSeatingMap();
                if (map == null || map.getLayoutData() == null) continue;

                // Parse the layout data
                SessionSeatingMapRequest layoutData = objectMapper.readValue(map.getLayoutData(), SessionSeatingMapRequest.class);

                // Update tier IDs in all seats
                updateTierIdsInLayout(layoutData, tierIdMapping);

                // Save the updated layout data
                map.setLayoutData(objectMapper.writeValueAsString(layoutData));
            } catch (IOException e) {
                log.error("Failed to update tier IDs in seating map", e);
                throw new RuntimeException("Failed to update tier IDs in seating map", e);
            }
        }
    }

    private void updateTierIdsInLayout(SessionSeatingMapRequest layoutData, Map<String, UUID> tierIdMapping) {
        if (layoutData == null || layoutData.getLayout() == null || layoutData.getLayout().getBlocks() == null) {
            return;
        }

        for (SessionSeatingMapRequest.Block block : layoutData.getLayout().getBlocks()) {
            // Update tier ID for standing capacity blocks
            if ("standing_capacity".equals(block.getType()) && block.getTierId() != null) {
                UUID actualTierId = tierIdMapping.get(block.getTierId());
                if (actualTierId != null) {
                    block.setTierId(actualTierId.toString());
                }
            }

            // Update seated grid blocks
            if ("seated_grid".equals(block.getType()) && block.getRows() != null) {
                for (SessionSeatingMapRequest.Row row : block.getRows()) {
                    if (row.getSeats() != null) {
                        updateSeatTierIds(row.getSeats(), tierIdMapping);
                    }
                }
            }

            // Update seats in standing capacity blocks
            if (block.getSeats() != null) {
                updateSeatTierIds(block.getSeats(), tierIdMapping);
            }
        }
    }

    private void updateSeatTierIds(List<SessionSeatingMapRequest.Seat> seats, Map<String, UUID> tierIdMapping) {
        for (SessionSeatingMapRequest.Seat seat : seats) {
            if (seat.getTierId() != null) {
                UUID actualTierId = tierIdMapping.get(seat.getTierId());
                if (actualTierId != null) {
                    seat.setTierId(actualTierId.toString());
                }
            }
        }
    }
}