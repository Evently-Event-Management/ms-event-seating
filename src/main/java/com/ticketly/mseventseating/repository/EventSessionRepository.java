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
    List<EventSession> findByEventId(UUID eventId);
    List<EventSession> findByEventIdAndStatus(UUID eventId, SessionStatus status);
    List<EventSession> findByStartTimeBetween(OffsetDateTime start, OffsetDateTime end);
}
