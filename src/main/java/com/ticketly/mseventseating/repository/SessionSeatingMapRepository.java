package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.SessionSeatingMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SessionSeatingMapRepository extends JpaRepository<SessionSeatingMap, UUID> {

    /**
     * Calls the native PostgreSQL function 'update_seat_statuses' to perform
     * an efficient, selective update of seat statuses within the layout_data JSONB field.
     *
     * @param sessionId The UUID of the event session to update.
     * @param seatIds   A list of seat UUIDs whose status needs to be changed.
     * @param status    The new status to set for the specified seats (e.g., "BOOKED").
     */
    @Modifying
    @Query(value = "CALL update_seat_statuses(?1, ?2, ?3)", nativeQuery = true)
    void updateSeatStatusesInLayout(
            UUID sessionId,
            UUID[] seatIds,
            String status
    );

    /**
     * Calls the native PostgreSQL function 'validate_seat_statuses' to efficiently
     * count how many of the requested seats are not in 'AVAILABLE' status.
     *
     * @param sessionId The UUID of the event session.
     * @param seatIds   An array of seat UUIDs to validate.
     * @return The number of seats from the input array that are NOT available.
     */
    @Query(value = "SELECT validate_seat_statuses(?1, ?2)", nativeQuery = true)
    int countUnavailableSeatsInLayout(UUID sessionId, UUID[] seatIds);
}