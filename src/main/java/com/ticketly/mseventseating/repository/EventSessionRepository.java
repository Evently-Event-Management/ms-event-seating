package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.EventSession;
import model.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT es FROM EventSession es JOIN FETCH es.event e JOIN FETCH e.organization WHERE es.id = :sessionId AND es.event.id = :eventId")
    Optional<EventSession> findByIdAndEventIdWithEvent(@Param("sessionId") UUID sessionId, @Param("eventId") UUID eventId);

    List<EventSession> findAllByIdIn(List<UUID> ids);


    List<EventSession> findAllByEventId(UUID eventId);

    boolean existsByEventIdAndStatus(UUID eventId, SessionStatus status);
    
    /**
     * Remove all entries from discount_sessions join table for a given session
     * This prevents foreign key constraint violations when deleting a session
     */
    @Modifying
    @Query(value = "DELETE FROM discount_sessions WHERE session_id = :sessionId", nativeQuery = true)
    void removeSessionFromDiscounts(@Param("sessionId") UUID sessionId);

    /**
     * Count sessions grouped by status for a specific event
     * 
     * @param eventId The event ID to get session counts for
     * @return List of counts by status
     */
    @Query("SELECT s.status as status, COUNT(s) as count FROM EventSession s WHERE s.event.id = :eventId GROUP BY s.status")
    List<Object[]> countSessionsByStatusForEvent(@Param("eventId") UUID eventId);
    
    /**
     * Count sessions grouped by status for a specific organization
     * 
     * @param organizationId The organization ID to get session counts for
     * @return List of counts by status
     */
    @Query("SELECT s.status as status, COUNT(s) as count FROM EventSession s JOIN s.event e WHERE e.organization.id = :organizationId GROUP BY s.status")
    List<Object[]> countSessionsByStatusForOrganization(@Param("organizationId") UUID organizationId);
    
    /**
     * Count total sessions for an event
     * 
     * @param eventId The event ID
     * @return Total number of sessions
     */
    @Query("SELECT COUNT(s) FROM EventSession s WHERE s.event.id = :eventId")
    Long countSessionsForEvent(@Param("eventId") UUID eventId);
    
    /**
     * Count total sessions for an organization
     * 
     * @param organizationId The organization ID
     * @return Total number of sessions
     */
    @Query("SELECT COUNT(s) FROM EventSession s JOIN s.event e WHERE e.organization.id = :organizationId")
    Long countSessionsForOrganization(@Param("organizationId") UUID organizationId);
    
    /**
     * Find all sessions for an organization with their event details
     * 
     * @param organizationId The organization ID
     * @param status Optional status filter
     * @param pageable Pagination and sorting information
     * @return Page of sessions with event details
     */
    @Query("SELECT new com.ticketly.mseventseating.dto.session.OrganizationSessionDTO(" +
           "s.id, s.startTime, s.endTime, s.salesStartTime, s.sessionType, s.status, " +
           "e.id, e.title, e.status, c.name) " +
           "FROM EventSession s " +
           "JOIN s.event e " +
           "LEFT JOIN e.category c " +
           "WHERE e.organization.id = :organizationId " +
           "AND (:status IS NULL OR s.status = :status)")
    Page<com.ticketly.mseventseating.dto.session.OrganizationSessionDTO> findSessionsByOrganization(
            @Param("organizationId") UUID organizationId, 
            @Param("status") SessionStatus status,
            Pageable pageable);
}
