package com.ticketly.mseventseating.service.order;

import com.ticketly.mseventseating.dto.event.OrderUpdatedEventDto;
import com.ticketly.mseventseating.dto.event.SeatStatusChangeEventDto;
import com.ticketly.mseventseating.model.Discount;
import com.ticketly.mseventseating.repository.DiscountRepository;
import com.ticketly.mseventseating.service.seat.SeatBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProcessingService {

    private final SeatBookingService seatBookingService;
    private final DiscountRepository discountRepository;

    /**
     * Process an order update event
     * Only processes orders with "completed" status
     */
    @Transactional
    public void processOrderUpdate(OrderUpdatedEventDto event) {
        if (event == null || event.SessionID() == null) {
            log.warn("Received an invalid OrderUpdated event. Ignoring.");
            return;
        }

        // Check if order ID is valid
        if (event.OrderID() == null) {
            log.warn("Received order update event with null order ID. Ignoring.");
            return;
        }
        
        // Only process completed orders
        if (!"completed".equalsIgnoreCase(event.Status())) {
            log.info("Ignoring order {} with status {}, only processing 'completed' orders", 
                    event.OrderID(), event.Status());
            return;
        }

        log.info("Processing completed order: {}", event.OrderID());

        // Extract seat IDs from tickets
        List<UUID> seatIds = event.tickets().stream()
                .map(OrderUpdatedEventDto.TicketDto::seat_id)
                .collect(Collectors.toList());

        if (seatIds.isEmpty()) {
            log.warn("Order {} has no seats to book. Ignoring.", event.OrderID());
            return;
        }

        // Book the seats
        SeatStatusChangeEventDto seatEvent = new SeatStatusChangeEventDto(event.SessionID(), seatIds);
        seatBookingService.processSeatsBooked(seatEvent);
        log.info("Booked {} seats for order: {}", seatIds.size(), event.OrderID());

        // Handle discount if present (and not empty)
        if (event.DiscountID() != null && !event.DiscountID().toString().isEmpty()) {
            updateDiscountUsage(event);
        } else {
            log.info("No discount applied to order {}", event.OrderID());
        }
    }

    /**
     * Updates discount usage count and total amount
     */
    private void updateDiscountUsage(OrderUpdatedEventDto event) {
        log.info("Updating discount usage for discount: {} (code: {}) in order: {}", 
                event.DiscountID(), event.DiscountCode(), event.OrderID());
        
        // Additional validation to ensure we have a valid discount ID
        if (event.DiscountID() == null || event.DiscountID().toString().isEmpty()) {
            log.warn("Empty or null discount ID for order {}. Skipping discount update.", event.OrderID());
            return;
        }
        
        Discount discount = discountRepository.findById(event.DiscountID())
                .orElse(null);
        
        if (discount == null) {
            log.warn("Discount {} not found for order {}. Skipping discount update.", 
                    event.DiscountID(), event.OrderID());
            return;
        }
        
        // Verify discount code matches if provided
        if (event.DiscountCode() != null && !event.DiscountCode().isEmpty() && 
                !event.DiscountCode().equals(discount.getCode())) {
            log.warn("Discount code mismatch for order {}. Expected: {}, Got: {}. Proceeding anyway.",
                    event.OrderID(), discount.getCode(), event.DiscountCode());
        }
        
        // Increment usage count
        discount.setCurrentUsage(discount.getCurrentUsage() + 1);
        
        // Update discounted total
        BigDecimal currentTotal = discount.getDiscountedTotal();
        if (currentTotal == null) {
            currentTotal = BigDecimal.ZERO;
        }
        
        // Add this order's discount amount to the running total
        if (event.DiscountAmount() != null && event.DiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discount.setDiscountedTotal(currentTotal.add(event.DiscountAmount()));
            log.info("Added discount amount {} to total for discount {}", 
                    event.DiscountAmount(), event.DiscountID());
        } else {
            log.info("No discount amount to add for order {} (amount: {})", 
                    event.OrderID(), event.DiscountAmount());
            // Keep the existing total
            discount.setDiscountedTotal(currentTotal);
        }
        
        discountRepository.save(discount);
        log.info("Updated discount {}: usage count={}, discounted total={}", 
                discount.getId(), discount.getCurrentUsage(), discount.getDiscountedTotal());
    }
}