package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.dto.event.SeatStatusChangeEventDto;
import com.ticketly.mseventseating.repository.SessionSeatingMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.SeatStatus; // Assuming your enum is in this package
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatBookingService {

    private final SessionSeatingMapRepository seatingMapRepository;

    /**
     * Processes the seat booking event by updating the status of the specified seats
     * to BOOKED within the session's seating map.
     * <p>
     * This operation is transactional. If the database update fails, the transaction
     * will be rolled back.
     *
     * @param event The DTO containing the session ID and the list of seat IDs to book.
     */
    @Transactional
    public void processSeatsBooked(SeatStatusChangeEventDto event) {
        if (event == null || event.sessionId() == null || event.seatIds() == null || event.seatIds().isEmpty()) {
            log.warn("Received an invalid or empty SeatsBooked event. Ignoring.");
            return;
        }

        log.info("Processing booking for {} seats in session {}", event.seatIds().size(), event.sessionId());

        UUID[] seatIdsArray = event.seatIds().toArray(new UUID[0]);

        seatingMapRepository.updateSeatStatusesInLayout(
                event.sessionId(),
                seatIdsArray, // Pass the new array
                SeatStatus.BOOKED.name()
        );

        log.info("Successfully updated seat statuses to BOOKED for session {}", event.sessionId());
    }
}