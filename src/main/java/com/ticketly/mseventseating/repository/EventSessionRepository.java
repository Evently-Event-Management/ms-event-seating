package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventSessionRepository extends JpaRepository<EventSession, UUID> {
    /**
     * Find all sessions for a specific event
     */
    List<EventSession> findByEventId(UUID eventId);

    /**
     * Find all sessions for a specific event with a given status
     */
    List<EventSession> findByEventIdAndStatus(UUID eventId, SessionStatus status);

    /**
     * Find sessions scheduled to start between specified dates
     */
    List<EventSession> findByStartTimeBetween(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Find sessions that should be put on sale at or before the given time
     */
    List<EventSession> findByStatusAndSalesStartFixedDatetimeLessThanEqual(
            SessionStatus status, OffsetDateTime dateTime);
}
