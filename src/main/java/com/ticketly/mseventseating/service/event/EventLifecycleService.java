package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.InvalidStateException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventStatus;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.SessionStatus;
import com.ticketly.mseventseating.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EventLifecycleService {

    private final EventRepository eventRepository;
    private final EventSchedulingService schedulingService;

    /**
     * Approves a pending event, making it live.
     * It also cancels any sessions that are already in the past.
     */
    public void approveEvent(UUID eventId) {
        Event event = findEventById(eventId);

        if (event.getStatus() != EventStatus.PENDING) {
            throw new InvalidStateException("Only events with PENDING status can be approved.");
        }

        event.setStatus(EventStatus.APPROVED);

        // Handle sessions: cancel past sessions, schedule future ones
        for (EventSession session : event.getSessions()) {
            if (session.getEndTime().isBefore(OffsetDateTime.now())) {
                session.setStatus(SessionStatus.CANCELLED);
                log.warn("Session {} for event {} is in the past and has been automatically cancelled upon approval.",
                        session.getId(), event.getId());
            }
        }

        // Schedule the on-sale jobs for the approved, future sessions
        schedulingService.scheduleOnSaleJobsForEvent(event);

        eventRepository.save(event);
        log.info("Event with ID {} has been approved.", eventId);
    }

    /**
     * Rejects a pending event submission.
     */
    public void rejectEvent(UUID eventId, String reason) {
        Event event = findEventById(eventId);

        if (event.getStatus() != EventStatus.PENDING) {
            throw new InvalidStateException("Only events with PENDING status can be rejected.");
        }

        event.setStatus(EventStatus.REJECTED);
        event.setRejectionReason(reason);
        eventRepository.save(event);
        log.info("Event with ID {} has been rejected. Reason: {}", eventId, reason);
    }

    private Event findEventById(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
    }
}
