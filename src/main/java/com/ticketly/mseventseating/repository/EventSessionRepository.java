package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.EventSession;
import model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventSessionRepository extends JpaRepository<EventSession, UUID> {

    /**
     * Counts the number of sessions for an event that are NOT in the completed states (SOLD_OUT or CANCELLED).
     * Returns 0 if all sessions are in completed states.
     *
     * @param eventId The event ID to check
     * @param completedStatuses List of session statuses considered as completed
     * @return Count of sessions that are not in completed states
     */
    @Query("SELECT COUNT(s) FROM EventSession s WHERE s.event.id = :eventId AND s.status NOT IN :completedStatuses")
    long countByEventIdAndStatusNotIn(@Param("eventId") UUID eventId, @Param("completedStatuses") List<SessionStatus> completedStatuses);

    /**
     * Checks if an event has any sessions (returns true if it has at least one session).
     *
     * @param eventId The event ID to check
     * @return true if the event has at least one session, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM EventSession s WHERE s.event.id = :eventId")
    boolean existsByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT es FROM EventSession es JOIN FETCH es.event WHERE es.id = :sessionId AND es.event.id = :eventId")
    Optional<EventSession> findByIdAndEventIdWithEvent(UUID sessionId, UUID eventId);

    List<EventSession> findAllByIdIn(List<UUID> ids);
}
