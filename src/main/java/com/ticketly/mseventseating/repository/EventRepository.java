package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    /**
     * Counts the number of active events for a given organization.
     * Used for tier limit validation.
     * @param organizationId The organization's ID.
     * @param status The status to count (typically APPROVED).
     * @return The number of events matching the criteria.
     */
    long countByOrganizationIdAndStatus(UUID organizationId, EventStatus status);
}