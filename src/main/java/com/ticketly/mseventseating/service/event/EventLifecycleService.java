package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.InvalidStateException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.LimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
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
    private final EventOwnershipService eventOwnershipService;
    private final LimitService limitService;


    /**
     * Approves a pending event submission.
     * * This method can only be called by an admin or a user who is not the owner of the event's organization.
     * * It changes the event status to APPROVED and handles session scheduling.
     * * Past sessions are automatically cancelled, and future sessions are scheduled for on-sale.
     */
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
            // If session start time is in the past, mark it as cancelled
            if (session.getStartTime().isBefore(now)) {
                session.setStatus(SessionStatus.CANCELLED);
                cancelledSessions++;
                log.warn("Session {} for event {} start time is in the past and has been automatically cancelled upon approval.",
                        session.getId(), event.getId());
            } else {
                // Session is in the future, mark as scheduled regardless of the on-sale time
                session.setStatus(SessionStatus.SCHEDULED);
                scheduledSessions++;
                log.debug("Session {} for event {} is in the future and will be scheduled for on-sale.",
                        session.getId(), event.getId());
            }
        }

        log.debug("Event {} has {} cancelled past sessions and {} future sessions to schedule",
                eventId, cancelledSessions, scheduledSessions);

        // Schedule the on-sale jobs for the approved, future sessions
        log.debug("Scheduling on-sale jobs for event {}", eventId);
        schedulingService.scheduleOnSaleJobsForEvent(event);

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
     * Deletes an event.
     * Only events with PENDING status can be deleted, and only by the organization owner.
     *
     * @param eventId the event to delete
     * @param userId  the ID of the user performing the deletion
     */
    public void deleteEvent(UUID eventId, String userId) {
        log.info("Deleting event ID: {} by user: {}", eventId, userId);

        // Only organization owners can delete events, no admin bypass
        log.debug("Verifying ownership for event {}", eventId);
        if (!eventOwnershipService.isOwner(eventId, userId)) {
            log.warn("User {} is not the owner of event {}", userId, eventId);
            throw new AuthorizationDeniedException("You are not authorized to delete this event.");
        }
        Event event = findEventById(eventId);

        if (event.getStatus() != EventStatus.PENDING) {
            log.warn("Cannot delete event {} with status {}", eventId, event.getStatus());
            throw new InvalidStateException("Only events with PENDING status can be deleted.");
        }

        // Delete the event and all its children (cascade delete)
        log.debug("Deleting event {} with {} sessions and {} tiers",
                eventId, event.getSessions().size(), event.getTiers().size());
        eventRepository.delete(event);
        eventOwnershipService.evictEventCacheById(eventId);
        log.info("Event with ID {} has been successfully deleted by user {}", eventId, userId);
    }

    private Event findEventById(UUID eventId) {
        log.debug("Finding event by ID: {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Event not found with ID: {}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });
    }
}
