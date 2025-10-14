package com.ticketly.mseventseating.service.discount;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.DiscountResponseDTO;
import com.ticketly.mseventseating.dto.event.DiscountRequestDTO;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.exception.ValidationException;
import com.ticketly.mseventseating.model.Discount;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.Tier;
import com.ticketly.mseventseating.model.discount.DiscountParameters;
import com.ticketly.mseventseating.repository.DiscountRepository;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.repository.EventSessionRepository;
import com.ticketly.mseventseating.repository.TierRepository;
import com.ticketly.mseventseating.service.event.EventOwnershipService;
import com.ticketly.mseventseating.service.projection.EventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final EventRepository eventRepository;
    private final TierRepository tierRepository;
    private final EventSessionRepository sessionRepository;
    private final EventOwnershipService eventOwnershipService;
    private final EventMapper eventMapper;
    private final ObjectMapper objectMapper;

    /**
     * Create a new discount for an event
     */
    @Transactional
    public DiscountResponseDTO createDiscount(UUID eventId, DiscountRequestDTO requestDTO, String userId) {
        log.info("Creating discount for event: {}, user: {}, request: {}", eventId, userId, requestDTO);

        // Verify ownership
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to create discount for event {} they don't own", userId, eventId);
            throw new ValidationException("You don't have permission to create discounts for this event");
        }

        // Get the event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event not found with ID: {}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });

        // Build discount
        Discount discount = buildDiscountFromRequest(requestDTO, event);

        // Save and return
        Discount savedDiscount = discountRepository.save(discount);
        log.info("Created discount: {} for event: {}", savedDiscount.getId(), eventId);

        return eventMapper.mapToDiscountDetailsDTO(savedDiscount);
    }
    
    /**
     * Get all discounts for an event
     */
    @Transactional(readOnly = true)
    public List<DiscountResponseDTO> getDiscounts(UUID eventId, boolean includePrivate, String userId) {
        log.info("Getting discounts for event: {}, includePrivate: {}, user: {}", eventId, includePrivate, userId);

        // If requesting private discounts, verify ownership
        if (includePrivate && !eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to access private discounts for event {} they don't own", userId, eventId);
            throw new ValidationException("You don't have permission to view private discounts for this event");
        }

        List<Discount> discounts;
        if (includePrivate) {
            discounts = discountRepository.findAllByEventId(eventId);
        } else {
            discounts = discountRepository.findAllByEventIdAndIsPublic(eventId, true);
        }

        log.debug("Found {} discounts for event: {}", discounts.size(), eventId);

        // Map to DTOs
        List<DiscountResponseDTO> dtoList = new ArrayList<>();
        for (Discount discount : discounts) {
            dtoList.add(eventMapper.mapToDiscountDetailsDTO(discount));
        }
        return dtoList;
    }
    
    /**
     * Get a specific discount
     */
    @Transactional(readOnly = true)
    public DiscountResponseDTO getDiscount(UUID eventId, UUID discountId, String userId) {
        log.info("Getting discount: {} for event: {}, user: {}", discountId, eventId, userId);

        Discount discount = getAndValidateDiscount(eventId, discountId);

        // If discount is private, verify ownership
        if (!discount.isPublic() && !eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to access private discount for event {} they don't own", userId, eventId);
            throw new ValidationException("You don't have permission to view this private discount");
        }

        return eventMapper.mapToDiscountDetailsDTO(discount);
    }
    
    /**
     * Update a discount
     */
    @Transactional
    public DiscountResponseDTO updateDiscount(UUID eventId, UUID discountId, DiscountRequestDTO requestDTO, String userId) {
        log.info("Updating discount: {} for event: {}, user: {}, request: {}", discountId, eventId, userId, requestDTO);

        // Verify ownership
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to update discount for event {} they don't own", userId, eventId);
            throw new ValidationException("You don't have permission to update discounts for this event");
        }

        Discount existingDiscount = getAndValidateDiscount(eventId, discountId);

        // Update fields but keep the same ID and event reference
        updateDiscountFromRequest(existingDiscount, requestDTO);

        Discount updatedDiscount = discountRepository.save(existingDiscount);
        log.info("Updated discount: {} for event: {}", discountId, eventId);

        return eventMapper.mapToDiscountDetailsDTO(updatedDiscount);
    }
    
    /**
     * Delete a discount
     */
    @Transactional
    public void deleteDiscount(UUID eventId, UUID discountId, String userId) {
        log.info("Deleting discount: {} for event: {}, user: {}", discountId, eventId, userId);

        // Verify ownership
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to delete discount for event {} they don't own", userId, eventId);
            throw new ValidationException("You don't have permission to delete discounts for this event");
        }

        Discount discount = getAndValidateDiscount(eventId, discountId);

        // Clear the many-to-many relationships to avoid foreign key constraint violations
        if (discount.getApplicableSessions() != null) {
            discount.getApplicableSessions().clear();
        }
        if (discount.getApplicableTiers() != null) {
            discount.getApplicableTiers().clear();
        }

        // Flush changes to ensure join table entries are deleted before the discount is deleted
        discountRepository.saveAndFlush(discount);

        discountRepository.delete(discount);
        log.info("Deleted discount: {} for event: {}", discountId, eventId);
    }
    
    /**
     * Validate a discount code
     */
    @Transactional(readOnly = true)
    public void validateDiscount(UUID discountId, UUID eventId) {
        log.info("Validating discount: {} for event: {}", discountId, eventId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> {
                    log.error("Discount not found with ID: {}", discountId);
                    return new ValidationException("Discount not found.");
                });

        // Check if the discount belongs to the correct event
        if (!discount.getEvent().getId().equals(eventId)) {
            log.warn("Discount {} does not belong to event {}", discountId, eventId);
            throw new ValidationException("Discount is not valid for this event.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean isDiscountActive = discount.isActive() &&
                (discount.getActiveFrom() == null || !discount.getActiveFrom().isAfter(now)) &&
                (discount.getExpiresAt() == null || !discount.getExpiresAt().isBefore(now));

        if (!isDiscountActive) {
            log.warn("Discount {} is not currently active or has expired", discountId);
            throw new ValidationException("Discount is not currently active or has expired.");
        }

        // Validate usage limit
        if (discount.getMaxUsage() != null && discount.getCurrentUsage() >= discount.getMaxUsage()) {
            log.warn("Discount {} usage limit reached (current: {}, max: {})", discountId, discount.getCurrentUsage(), discount.getMaxUsage());
            throw new ValidationException("Discount usage limit has been reached.");
        }
        log.info("Discount {} is valid for event {}", discountId, eventId);
    }
    
    /**
     * Increment the usage count for a discount
     */
    @Transactional
    public void incrementUsageCount(UUID discountId) {
        log.info("Incrementing usage count for discount: {}", discountId);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> {
                    log.error("Discount not found with ID: {}", discountId);
                    return new ResourceNotFoundException("Discount not found with ID: " + discountId);
                });

        discount.setCurrentUsage(discount.getCurrentUsage() + 1);
        discountRepository.save(discount);
        log.info("Incremented usage count for discount: {}, new count: {}",
                discountId, discount.getCurrentUsage());
    }
    
    // Helper methods
    
    private Discount getAndValidateDiscount(UUID eventId, UUID discountId) {
        log.debug("Validating discount: {} for event: {}", discountId, eventId);
        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> {
                    log.error("Discount not found with ID: {}", discountId);
                    return new ResourceNotFoundException("Discount not found with ID: " + discountId);
                });

        if (!discount.getEvent().getId().equals(eventId)) {
            log.warn("Discount {} does not belong to event {}", discountId, eventId);
            throw new ValidationException("Discount does not belong to the specified event");
        }

        return discount;
    }
    
    private Discount buildDiscountFromRequest(DiscountRequestDTO requestDTO, Event event) {
        log.debug("Building discount from request for event: {}", event.getId());
        List<Tier> tiers = new ArrayList<>();
        if (requestDTO.getApplicableTierIds() != null && !requestDTO.getApplicableTierIds().isEmpty()) {
            List<UUID> tierIds = new ArrayList<>(requestDTO.getApplicableTierIds());
            tiers = tierRepository.findAllByIdIn(tierIds);

            // Validate tiers belong to this event
            tiers.forEach(tier -> {
                if (!tier.getEvent().getId().equals(event.getId())) {
                    log.warn("Tier {} does not belong to event {}", tier.getId(), event.getId());
                    throw new ValidationException("Tier " + tier.getId() + " does not belong to this event");
                }
            });
        }

        List<EventSession> sessions = new ArrayList<>();
        if (requestDTO.getApplicableSessionIds() != null && !requestDTO.getApplicableSessionIds().isEmpty()) {
            List<UUID> sessionIds = new ArrayList<>(requestDTO.getApplicableSessionIds());
            sessions = sessionRepository.findAllByIdIn(sessionIds);

            // Validate sessions belong to this event
            sessions.forEach(session -> {
                if (!session.getEvent().getId().equals(event.getId())) {
                    log.warn("Session {} does not belong to event {}", session.getId(), event.getId());
                    throw new ValidationException("Session " + session.getId() + " does not belong to this event");
                }
            });
        }

        DiscountParameters parameters = objectMapper.convertValue(requestDTO.getParameters(), DiscountParameters.class);

        return Discount.builder()
                .id(UUID.randomUUID())
                .event(event)
                .code(requestDTO.getCode())
                .parameters(parameters)
                .maxUsage(requestDTO.getMaxUsage())
                .currentUsage(0) // New discounts start with 0 usage
                .isActive(requestDTO.isActive())
                .isPublic(requestDTO.isPublic())
                .activeFrom(requestDTO.getActiveFrom())
                .expiresAt(requestDTO.getExpiresAt())
                .applicableTiers(tiers)
                .applicableSessions(sessions)
                .build();
    }
    
    private void updateDiscountFromRequest(Discount discount, DiscountRequestDTO requestDTO) {
        log.debug("Updating discount {} from request", discount.getId());
        discount.setCode(requestDTO.getCode());
        discount.setParameters(
                objectMapper.convertValue(requestDTO.getParameters(), DiscountParameters.class));
        discount.setMaxUsage(requestDTO.getMaxUsage());
        discount.setActive(requestDTO.isActive());
        discount.setPublic(requestDTO.isPublic());
        discount.setActiveFrom(requestDTO.getActiveFrom());
        discount.setExpiresAt(requestDTO.getExpiresAt());

        // Update applicable tiers if provided
        if (requestDTO.getApplicableTierIds() != null) {
            List<UUID> tierIds = new ArrayList<>(requestDTO.getApplicableTierIds());
            List<Tier> tiers = tierRepository.findAllByIdIn(tierIds);

            // Validate tiers belong to this event
            Event event = discount.getEvent();
            tiers.forEach(tier -> {
                if (!tier.getEvent().getId().equals(event.getId())) {
                    log.warn("Tier {} does not belong to event {}", tier.getId(), event.getId());
                    throw new ValidationException("Tier " + tier.getId() + " does not belong to this event");
                }
            });

            discount.setApplicableTiers(tiers);
        }

        // Update applicable sessions if provided
        if (requestDTO.getApplicableSessionIds() != null) {
            List<UUID> sessionIds = new ArrayList<>(requestDTO.getApplicableSessionIds());
            List<EventSession> sessions = sessionRepository.findAllByIdIn(sessionIds);

            // Validate sessions belong to this event
            Event event = discount.getEvent();
            sessions.forEach(session -> {
                if (!session.getEvent().getId().equals(event.getId())) {
                    log.warn("Session {} does not belong to event {}", session.getId(), event.getId());
                    throw new ValidationException("Session " + session.getId() + " does not belong to this event");
                }
            });

            discount.setApplicableSessions(sessions);
        }
    }
}
