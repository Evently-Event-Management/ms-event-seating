package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.SessionSeatingMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    @Query(value = "SELECT update_seat_statuses(:sessionId, :seatIds, :status)", nativeQuery = true)
    void updateSeatStatusesInLayout(
            @Param("sessionId") UUID sessionId,
            @Param("seatIds") List<UUID> seatIds,
            @Param("status") String status
    );
}