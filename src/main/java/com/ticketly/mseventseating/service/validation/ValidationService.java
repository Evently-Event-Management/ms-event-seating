package com.ticketly.mseventseating.service.validation;

import com.ticketly.mseventseating.exception.ValidationException;
import com.ticketly.mseventseating.model.Discount;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.repository.DiscountRepository;
import com.ticketly.mseventseating.repository.EventSessionRepository;
import com.ticketly.mseventseating.repository.SessionSeatingMapRepository;
import dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import model.EventStatus;
import model.SessionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ValidationService {

    private final EventSessionRepository eventSessionRepository;
    private final DiscountRepository discountRepository;
    private final SessionSeatingMapRepository seatingMapRepository;

    @Transactional(readOnly = true)
    public void validatePreOrder(CreateOrderRequest request) {
        // 1. Fetch Session and Event in one go
        EventSession session = eventSessionRepository
                .findByIdAndEventIdWithEvent(request.getSession_id(), request.getEvent_id())
                .orElseThrow(() -> new ValidationException("Session or Event not found."));

        // 2. Validate Event and Session Status
        if (session.getEvent().getStatus() != EventStatus.APPROVED) {
            throw new ValidationException("Event is not approved for sale.");
        }
        if (!session.getEvent().getOrganization().getId().equals(request.getOrganization_id())) {
            throw new ValidationException("Event does not belong to the specified organization.");
        }
        if (session.getStatus() != SessionStatus.ON_SALE) {
            throw new ValidationException("Session is not currently on sale.");
        }

        // 3. Validate Discount (if provided)
        if (request.getDiscount_id() != null) {
            validateDiscount(request.getDiscount_id(), request.getEvent_id());
        }

        // 4. Validate Seats
        validateSeats(request.getSession_id(), request.getSeat_ids());
    }

    private void validateDiscount(UUID discountId, UUID eventId) {
        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ValidationException("Discount not found."));

        // Check if the discount belongs to the correct event
        if (!discount.getEvent().getId().equals(eventId)) {
            throw new ValidationException("Discount is not valid for this event.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean isDiscountActive = discount.isActive() &&
                (discount.getActiveFrom() == null || !discount.getActiveFrom().isAfter(now)) &&
                (discount.getExpiresAt() == null || !discount.getExpiresAt().isBefore(now));

        if (!isDiscountActive) {
            throw new ValidationException("Discount is not currently active or has expired.");
        }

        // Validate usage limit
        if (discount.getMaxUsage() != null && discount.getCurrentUsage() >= discount.getMaxUsage()) {
            throw new ValidationException("Discount usage limit has been reached.");
        }
    }

    private void validateSeats(UUID sessionId, List<UUID> seatIds) {
        // Convert List to UUID array for the native query
        UUID[] seatIdsArray = seatIds.toArray(new UUID[0]);

        // Call the efficient database function
        int unavailableCount = seatingMapRepository.countUnavailableSeatsInLayout(sessionId, seatIdsArray);

        // If the function returns a number greater than 0, validation fails.
        if (unavailableCount > 0) {
            throw new ValidationException("One or more selected seats are no longer available or do not exist.");
        }

        // If the count is 0, all seats are valid and available.
    }
}