package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.Event;
import model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    /**
     * Counts the number of active events for a given organization.
     * Used for tier limit validation.
     *
     * @param organizationId The organization's ID.
     * @param status         The status to count (typically APPROVED).
     * @return The number of events matching the criteria.
     */
    long countByOrganizationIdAndStatus(UUID organizationId, EventStatus status);

    /**
     * Finds all events with the specified status, with pagination support.
     *
     * @param status   The event status to filter by.
     * @param pageable The pagination information.
     * @return A page of events matching the given status.
     */
    Page<Event> findAllByStatus(EventStatus status, Pageable pageable);

    /**
     * Finds all events with title or description containing the search term,
     * optionally filtered by status.
     *
     * @param searchTerm The term to search for in title or description.
     * @param status     The event status to filter by (can be null).
     * @param pageable   The pagination information.
     * @return A page of events matching the search criteria.
     */
    @Query("SELECT e FROM Event e WHERE " +
            "(:searchTerm IS NULL OR " +
            "LOWER(COALESCE(e.title, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) OR " +
            "LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))) AND " +
            "(:status IS NULL OR e.status = :status)")
    Page<Event> findBySearchTermAndStatus(
            @Param("searchTerm") String searchTerm,
            @Param("status") EventStatus status,
            Pageable pageable);


    /**
     * Finds all events for a specific organization with optional search and status filtering.
     *
     * @param organizationId The organization ID to filter by.
     * @param searchTerm     The term to search for in title or description (optional).
     * @param status         The event status to filter by (optional).
     * @param pageable       The pagination information.
     * @return A page of events matching the criteria.
     */
    @Query("SELECT e FROM Event e WHERE e.organization.id = :organizationId AND " +
            "(:searchTerm IS NULL OR " +
            "LOWER(COALESCE(e.title, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) OR " +
            "LOWER(COALESCE(e.description, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))) AND " +
            "(:status IS NULL OR e.status = :status)")
    Page<Event> findByOrganizationIdAndSearchTermAndStatus(
            @Param("organizationId") UUID organizationId,
            @Param("searchTerm") String searchTerm,
            @Param("status") EventStatus status,
            Pageable pageable);
}