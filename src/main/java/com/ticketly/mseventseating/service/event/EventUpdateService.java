package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.dto.event.UpdateEventRequest;
import com.ticketly.mseventseating.config.AppLimitsConfig;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Category;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventCoverPhoto;
import com.ticketly.mseventseating.repository.CategoryRepository;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for updating event details, specifically:
 * - title
 * - description
 * - overview
 * 
 * Cover photos are managed through separate methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventUpdateService {

    private final EventRepository eventRepository;
    private final S3StorageService s3StorageService;
    private final EventOwnershipService eventOwnershipService;
    private final CategoryRepository categoryRepository;
    private final AppLimitsConfig appLimitsConfig;

    /**
     * Updates basic information about an event (title, description, overview)
     * @param eventId The ID of the event to update
     * @param request The update request containing the new values
     * @param userId The ID of the user making the request
     * @return EventResponseDTO with the updated event data
     */
    @Transactional
    public EventResponseDTO updateEventDetails(UUID eventId, UpdateEventRequest request, String userId) {
        log.info("Updating event: {} with new details by user: {}", eventId, userId);
        
        // Verify ownership
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to update event {} they don't own", userId, eventId);
            throw new BadRequestException("You don't have permission to update this event");
        }
        
        // Get the event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event not found with ID: {}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });
        
        // Update the event details
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            event.setTitle(request.getTitle().trim());
        }
        
        // Update description if provided
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        
        // Update overview if provided
        if (request.getOverview() != null) {
            event.setOverview(request.getOverview());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> {
                        log.error("Category not found with ID: {}", request.getCategoryId());
                        return new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId());
                    });
            event.setCategory(category);
        }

        // Save the changes
        Event updatedEvent = eventRepository.save(event);
        log.info("Successfully updated event: {}", eventId);
        
        return mapToEventResponseDTO(updatedEvent);
    }
    
    /**
     * Adds a new cover photo to an event
     * @param eventId The ID of the event
     * @param coverImage The cover image file to upload
     * @param userId The ID of the user making the request
     * @return EventResponseDTO with the updated event data
     */
    @Transactional
    public EventResponseDTO addCoverPhoto(UUID eventId, MultipartFile coverImage, String userId) {
        log.info("Adding cover photo to event: {} by user: {}", eventId, userId);
        
        // Verify ownership
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to add cover photo to event {} they don't own", userId, eventId);
            throw new BadRequestException("You don't have permission to update this event");
        }
        
        // Get the event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event not found with ID: {}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });
        
        // Check if the event has reached the maximum number of cover photos
        int maxCoverPhotos = appLimitsConfig.getEvent().getMaxCoverPhotos();
        int currentPhotos = event.getCoverPhotos() != null ? event.getCoverPhotos().size() : 0;
        
        log.debug("Cover photo limit check: current={}, max={}", currentPhotos, maxCoverPhotos);
        
        if (currentPhotos >= maxCoverPhotos) {
            log.warn("Event {} already has the maximum number of cover photos ({}/{})", 
                    eventId, currentPhotos, maxCoverPhotos);
            throw new BadRequestException("This event already has the maximum allowed number of cover photos: " + maxCoverPhotos);
        }
        
        // Validate file
        validateCoverPhotoFile(coverImage);
        
        try {
            // Upload the new photo to S3
            String photoKey = s3StorageService.uploadFile(coverImage, "event-cover-photos");
            log.debug("Successfully uploaded image to S3, key: {}", photoKey);
            
            // Create a new EventCoverPhoto entity
            EventCoverPhoto coverPhoto = new EventCoverPhoto();
            coverPhoto.setEvent(event);
            coverPhoto.setPhotoUrl(photoKey);
            
            // Add to the event's photos
            if (event.getCoverPhotos() == null) {
                event.setCoverPhotos(new ArrayList<>());
            }
            event.getCoverPhotos().add(coverPhoto);
            
            // Save the changes
            Event updatedEvent = eventRepository.save(event);
            log.info("Successfully added cover photo to event: {}", eventId);
            
            return mapToEventResponseDTO(updatedEvent);
        } catch (IOException e) {
            log.error("Failed to upload cover image for event {}", eventId, e);
            throw new RuntimeException("Failed to upload cover image.", e);
        }
    }
    
    /**
     * Removes a cover photo from an event
     * @param eventId The ID of the event
     * @param photoId The ID of the cover photo to remove
     * @param userId The ID of the user making the request
     * @return EventResponseDTO with the updated event data
     */
    @Transactional
    public EventResponseDTO removeCoverPhoto(UUID eventId, UUID photoId, String userId) {
        log.info("Removing cover photo: {} from event: {} by user: {}", photoId, eventId, userId);
        
        // Verify ownership
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to remove cover photo from event {} they don't own", userId, eventId);
            throw new BadRequestException("You don't have permission to update this event");
        }
        
        // Get the event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event not found with ID: {}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });
        
        // Find and remove the photo
        if (event.getCoverPhotos() != null) {
            EventCoverPhoto photoToRemove = null;
            for (EventCoverPhoto photo : event.getCoverPhotos()) {
                if (photo.getId().equals(photoId)) {
                    photoToRemove = photo;
                    break;
                }
            }
            
            if (photoToRemove != null) {
                // Try to delete from S3 first
                try {
                    s3StorageService.deleteFile(photoToRemove.getPhotoUrl());
                    log.debug("Deleted photo from S3: {}", photoToRemove.getPhotoUrl());
                } catch (Exception e) {
                    log.warn("Failed to delete photo from S3: {}", photoToRemove.getPhotoUrl(), e);
                    // Continue with removal from database even if S3 delete fails
                }
                
                // Remove from the event's photos
                event.getCoverPhotos().remove(photoToRemove);
                
                // Save the changes
                Event updatedEvent = eventRepository.save(event);
                log.info("Successfully removed cover photo: {} from event: {}", photoId, eventId);
                
                return mapToEventResponseDTO(updatedEvent);
            } else {
                log.warn("Cover photo: {} not found for event: {}", photoId, eventId);
                throw new ResourceNotFoundException("Cover photo not found with ID: " + photoId);
            }
        } else {
            log.warn("Event: {} has no cover photos", eventId);
            throw new ResourceNotFoundException("Event does not have any cover photos");
        }
    }
    
    /**
     * Validates a cover photo file
     * @param file The file to validate
     * @throws BadRequestException if the file is invalid
     */
    private void validateCoverPhotoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cover photo file cannot be empty");
        }
        
        if (!Objects.requireNonNull(file.getContentType()).startsWith("image/")) {
            log.warn("Invalid file type detected: {}", file.getContentType());
            throw new BadRequestException("Invalid file type detected. Please upload only image files.");
        }
        
        // Use the configured maximum size from application.yml
        long maxSize = appLimitsConfig.getEvent().getMaxCoverPhotoSize();
        log.debug("Validating cover photo size: {}KB, max allowed: {}MB", 
                file.getSize() / 1024, maxSize / (1024 * 1024));
                
        if (file.getSize() > maxSize) {
            log.warn("File size too large: {}KB, max: {}KB", file.getSize() / 1024, maxSize / 1024);
            throw new BadRequestException("File size exceeds the maximum allowed size of " + (maxSize / (1024 * 1024)) + "MB");
        }
    }
    
    /**
     * Maps an Event entity to an EventResponseDTO
     * @param event The event entity
     * @return The DTO representation
     */
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