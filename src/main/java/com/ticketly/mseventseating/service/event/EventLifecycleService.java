package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.InvalidStateException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.exception.SchedulingException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.repository.EventSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.EventStatus;
import model.SessionStatus;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EventLifecycleService {

    private final EventRepository eventRepository;
    private final EventSessionRepository eventSessionRepository;
    private final EventOwnershipService eventOwnershipService;


    /**
     * Approves a pending event submission.
     * * This method can only be called by an admin or a user who is not the owner of the event's organization.
     * * It changes the event status to APPROVED and handles session scheduling.
     * * Past sessions are automatically cancelled, and future sessions are scheduled for on-sale.
     *
     * @throws SchedulingException if there's any failure in scheduling sessions, which will rollback the transaction
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveEvent(UUID eventId, String userId) {
        log.info("Approving event ID: {} by user: {}", eventId, userId);
        Event event = findEventById(eventId);

        // User cannot approve own event
        Organization organization = event.getOrganization();
        if (organization.getUserId().equals(userId)) {
            log.warn("User {} attempted to approve their own event {}", userId, eventId);
            throw new AuthorizationDeniedException("You cannot approve your own event.");
        }

        if (event.getStatus() != EventStatus.PENDING) {
            log.warn("Cannot approve event {} with status {}", eventId, event.getStatus());
            throw new InvalidStateException("Only events with PENDING status can be approved.");
        }

        log.debug("Changing event {} status from PENDING to APPROVED", eventId);
        event.setStatus(EventStatus.APPROVED);

        // Handle sessions: cancel past sessions, schedule future ones
        int cancelledSessions = 0;
        int scheduledSessions = 0;
        OffsetDateTime now = OffsetDateTime.now();

        for (EventSession session : event.getSessions()) {
            if (session.getStartTime().isBefore(now)) {
                session.setStatus(SessionStatus.CANCELLED);
                cancelledSessions++;
                log.warn("Session {} for event {} start time is in the past and has been automatically cancelled upon approval.",
                        session.getId(), event.getId());
            } else if (session.getSalesStartTime().isBefore(now)) {
                session.setStatus(SessionStatus.ON_SALE);
                scheduledSessions++;
                log.debug("Session {} for event {} sales start time is in the past, setting status to ON_SALE.",
                        session.getId(), event.getId());
            } else {
                session.setStatus(SessionStatus.SCHEDULED);
                scheduledSessions++;
                log.debug("Session {} for event {} is scheduled for future sale at {}, setting status to SCHEDULED.",
                        session.getId(), event.getId(), session.getSalesStartTime());
            }
        }

        log.debug("Event {} has {} cancelled past sessions and {} future sessions to schedule",
                eventId, cancelledSessions, scheduledSessions);

        eventRepository.save(event);
        log.info("Event with ID {} has been successfully approved with {} active sessions",
                eventId, scheduledSessions);
    }

    /**
     * Rejects a pending event submission.
     */
    public void rejectEvent(UUID eventId, String reason) {
        log.info("Rejecting event ID: {} with reason: '{}'", eventId, reason);
        Event event = findEventById(eventId);

        if (event.getStatus() != EventStatus.PENDING) {
            log.warn("Cannot reject event {} with status {}", eventId, event.getStatus());
            throw new InvalidStateException("Only events with PENDING status can be rejected.");
        }

        log.debug("Changing event {} status from PENDING to REJECTED", eventId);
        event.setStatus(EventStatus.REJECTED);
        event.setRejectionReason(reason);
        eventRepository.save(event);
        log.info("Event with ID {} has been successfully rejected. Reason: {}", eventId, reason);
    }

    /**
     * Deletes an event if it's in PENDING status.
     * Only the owner of the event can delete it.
     *
     * @param eventId UUID of the event to delete
     * @param userId ID of the user requesting deletion
     * @throws ResourceNotFoundException if the event doesn't exist
     * @throws AuthorizationDeniedException if the user is not the event owner
     * @throws InvalidStateException if the event is not in PENDING status
     */
    @Transactional
    public void deleteEvent(UUID eventId, String userId) {
        log.info("Attempting to delete event ID: {} by user: {}", eventId, userId);

        // 1. Check if user owns the event
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} attempted to delete event {} which they do not own", userId, eventId);
            throw new AuthorizationDeniedException("You are not authorized to delete this event");
        }

        // 2. ++ CHANGE: Check for ON_SALE sessions instead of PENDING status ++
        // This calls the new, efficient repository method.
        if (eventSessionRepository.existsByEventIdAndStatus(eventId, SessionStatus.ON_SALE)) {
            log.warn("Cannot delete event {}: it has one or more sessions that are ON_SALE", eventId);
            throw new InvalidStateException("Cannot delete an event that has sessions currently on sale.");
        }

        // 3. If the check passes, find the event to delete.
        // We still need to fetch the event to perform the delete operation.
        Event event = findEventById(eventId);

        log.debug("Deleting event {}: {}", eventId, event.getTitle());
        eventRepository.delete(event);
        log.info("Event with ID {} has been successfully deleted", eventId);
    }

    private Event findEventById(UUID eventId) {
        log.debug("Finding event by ID: {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Event not found with ID: {}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });
    }

    public void putSessionOnSale(UUID sessionId) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("EventSession not found with ID: " + sessionId));

        // You can add extra checks here, e.g., ensure the parent event is APPROVED
        if (session.getEvent().getStatus() != EventStatus.APPROVED) {
            log.warn("Attempted to put session {} on sale, but parent event {} is not APPROVED.",
                    sessionId, session.getEvent().getId());
            throw new InvalidStateException("Cannot put session on sale because the parent event is not APPROVED.");
        }

        // Check weather session start time is in  the past
        if (session.getStartTime().isBefore(OffsetDateTime.now())) {
            log.warn("Cannot put session {} on sale because its start time is in the past.", sessionId);
            throw new InvalidStateException("Cannot put session on sale because its start time is in the past.");
        }

        session.setStatus(SessionStatus.ON_SALE);
        eventSessionRepository.save(session);
        log.info("Session {} has been successfully put ON_SALE.", sessionId);
    }

    /**
     * Marks a session as CLOSED. This is typically called by the scheduled job after a session ends.
     * If all sessions for the parent event are now either CLOSED or CANCELLED, the event is marked as COMPLETED.
     *
     * @param sessionId The ID of the session to mark as CLOSED
     */
    public void markSessionAsClosed(UUID sessionId) {
        log.info("Marking session ID: {} as CLOSED", sessionId);
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("EventSession not found with ID: " + sessionId));

        // Check if session can be marked as CLOSED
        if (session.getStatus() != SessionStatus.ON_SALE && session.getStatus() != SessionStatus.SCHEDULED) {
            log.warn("Cannot mark session {} as CLOSED because its status is {}", sessionId, session.getStatus());
            throw new InvalidStateException("Cannot mark session as CLOSED because its current status is " + session.getStatus());
        }

        // Update session status
        session.setStatus(SessionStatus.CLOSED);
        eventSessionRepository.save(session);
        log.info("Session {} has been successfully marked as CLOSED", sessionId);

        // Check if the parent event should be marked as COMPLETED
        checkAndUpdateEventCompletion(session.getEvent().getId());
    }

    /**
     * Checks if all sessions for an event are either CLOSED or CANCELLED, and if so,
     * marks the event as COMPLETED.
     *
     * @param eventId The ID of the event to check
     */
    private void checkAndUpdateEventCompletion(UUID eventId) {
        log.debug("Checking if event ID: {} should be marked as COMPLETED", eventId);
        Event event = findEventById(eventId);

        // Only APPROVED events can be marked as COMPLETED
        if (event.getStatus() != EventStatus.APPROVED) {
            log.debug("Event {} has status {}, so it cannot be marked as COMPLETED yet", eventId, event.getStatus());
            return;
        }

        // Check if the event has any sessions
        boolean hasAnySessions = eventSessionRepository.existsByEventId(eventId);
        if (!hasAnySessions) {
            log.debug("Event {} has no sessions, cannot mark as COMPLETED", eventId);
            return;
        }

        // Define the completed session statuses
        List<SessionStatus> completedStatuses = List.of(SessionStatus.CLOSED, SessionStatus.CANCELLED);

        // Count sessions that are not in completed states
        long incompleteSessionsCount = eventSessionRepository.countByEventIdAndStatusNotIn(eventId, completedStatuses);

        // If no incomplete sessions are found, mark the event as COMPLETED
        if (incompleteSessionsCount == 0) {
            log.info("All sessions for event ID: {} are either CLOSED or CANCELLED, marking event as COMPLETED", eventId);
            event.setStatus(EventStatus.COMPLETED);
            eventRepository.save(event);
            log.info("Event with ID {} has been successfully marked as COMPLETED", eventId);
        } else {
            log.debug("Event ID: {} still has {} active sessions, status remains {}",
                    eventId, incompleteSessionsCount, event.getStatus());
        }
    }
}
