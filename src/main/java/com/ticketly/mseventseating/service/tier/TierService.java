package com.ticketly.mseventseating.service.tier;

import com.ticketly.mseventseating.dto.tier.CreateTierRequest;
import com.ticketly.mseventseating.dto.tier.TierResponseDTO;
import com.ticketly.mseventseating.dto.tier.UpdateTierRequest;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.Tier;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.repository.TierRepository;
import com.ticketly.mseventseating.service.event.EventOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing tier operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TierService {

    private final TierRepository tierRepository;
    private final EventRepository eventRepository;
    private final EventOwnershipService eventOwnershipService;

    /**
     * Creates a new tier for an event
     * 
     * @param eventId The ID of the event
     * @param request The tier creation request
     * @param userId The ID of the user making the request
     * @return The created tier as a response DTO
     */
    @Transactional
    public TierResponseDTO createTier(UUID eventId, CreateTierRequest request, String userId) {
        log.info("Creating tier for event: {} by user: {}", eventId, userId);
        
        // Verify ownership
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to create tier for event {} they don't own", userId, eventId);
            throw new BadRequestException("You don't have permission to update this event");
        }
        
        // Get the event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event not found with ID: {}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });
        
        // Create and save the new tier
        Tier tier = Tier.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .color(request.color())
                .price(request.price())
                .event(event)
                .build();
        
        Tier savedTier = tierRepository.save(tier);
        log.info("Successfully created tier: {} for event: {}", savedTier.getId(), eventId);
        
        return mapToTierResponseDTO(savedTier);
    }
    
    /**
     * Updates an existing tier
     * 
     * @param eventId The ID of the event
     * @param tierId The ID of the tier to update
     * @param request The tier update request
     * @param userId The ID of the user making the request
     * @return The updated tier as a response DTO
     */
    @Transactional
    public TierResponseDTO updateTier(UUID eventId, UUID tierId, UpdateTierRequest request, String userId) {
        log.info("Updating tier: {} for event: {} by user: {}", tierId, eventId, userId);
        
        // Verify ownership
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to update tier {} for event {} they don't own", userId, tierId, eventId);
            throw new BadRequestException("You don't have permission to update this event");
        }
        
        // Get the tier and verify it belongs to the specified event
        Tier tier = tierRepository.findById(tierId)
                .orElseThrow(() -> {
                    log.error("Tier not found with ID: {}", tierId);
                    return new ResourceNotFoundException("Tier not found with ID: " + tierId);
                });
        
        if (!tier.getEvent().getId().equals(eventId)) {
            log.warn("Tier {} does not belong to event {}", tierId, eventId);
            throw new BadRequestException("The specified tier does not belong to this event");
        }
        
        // Update the tier
        if (request.name() != null && !request.name().isBlank()) {
            tier.setName(request.name());
        }
        
        if (request.color() != null && !request.color().isBlank()) {
            tier.setColor(request.color());
        }
        
        if (request.price() != null) {
            tier.setPrice(request.price());
        }
        
        Tier updatedTier = tierRepository.save(tier);
        log.info("Successfully updated tier: {} for event: {}", tierId, eventId);
        
        return mapToTierResponseDTO(updatedTier);
    }
    
    /**
     * Maps a Tier entity to a TierResponseDTO
     * 
     * @param tier The tier entity
     * @return The tier response DTO
     */
    private TierResponseDTO mapToTierResponseDTO(Tier tier) {
        return new TierResponseDTO(
                tier.getId(),
                tier.getName(),
                tier.getColor(),
                tier.getPrice(),
                tier.getEvent().getId()
        );
    }
}
