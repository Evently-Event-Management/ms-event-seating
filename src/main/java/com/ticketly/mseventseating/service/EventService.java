package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.factory.EventFactory;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final OrganizationOwnershipService ownershipService;
    private final SubscriptionTierService tierService;
    private final EventFactory eventFactory;
    private final SchedulerClient schedulerClient; // For AWS EventBridge Scheduler

    @Value("${aws.scheduler.sqs-queue-arn}")
    private String sqsQueueArn;

    @Value("${aws.scheduler.role-arn}")
    private String schedulerRoleArn;

    @Value("${aws.scheduler.group-name}")
    private String schedulerGroupName;

    @Transactional
    public EventResponseDTO createEvent(CreateEventRequest request, String userId, Jwt jwt) {
        // 1. Authorization: Verify the user owns the target organization.
        Organization organization = ownershipService.verifyOwnershipAndGetOrganization(request.getOrganizationId(), userId);

        // 2. Business Rule Validation: Check if the user's tier allows creating this event.
        validateTierLimits(organization.getId(), jwt, request.getSessions().size());

        // 3. Object Creation: Delegate the complex construction to the factory.
        Event event = eventFactory.createFromRequest(request, organization);

        // 4. Persistence: Save the entire event aggregate (cascades to sessions, tiers, etc.).
        Event savedEvent = eventRepository.save(event);
        log.info("Created new event with ID: {}", savedEvent.getId());

        // 5. Orchestration (Side Effects): Schedule the on-sale jobs for each session.
        scheduleOnSaleJobsForEvent(savedEvent);

        // 6. Return Response DTO
        return mapToEventResponseDTO(savedEvent);
    }

    private void validateTierLimits(UUID organizationId, Jwt jwt, int numSessions) {
        int maxActiveEvents = tierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        long currentActiveEvents = eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
        if (currentActiveEvents >= maxActiveEvents) {
            throw new BadRequestException("You have reached the limit of " + maxActiveEvents + " active events for your tier.");
        }

        int maxSessions = tierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        if (numSessions > maxSessions) {
            throw new BadRequestException("You cannot create more than " + maxSessions + " sessions per event for your tier.");
        }
    }

    private void scheduleOnSaleJobsForEvent(Event event) {
        for (EventSession session : event.getSessions()) {
            Instant scheduleTime = calculateSaleStartTime(session);
            // Only schedule if the sale time is in the future
            if (scheduleTime.isAfter(Instant.now())) {
                createEventBridgeSchedule(session, scheduleTime);
            }
        }
    }

    private Instant calculateSaleStartTime(EventSession session) {
        return switch (session.getSalesStartRuleType()) {
            case IMMEDIATE -> Instant.now();
            case FIXED -> session.getSalesStartFixedDatetime().toInstant();
            case ROLLING -> {
                if (session.getSalesStartHoursBefore() == null) {
                    throw new BadRequestException("Sales start hours must be provided for rolling sales rule.");
                }
                yield session.getStartTime().minusHours(session.getSalesStartHoursBefore()).toInstant();
            }
        };
    }

    private void createEventBridgeSchedule(EventSession session, Instant scheduleTime) {
        try {
            // Format time for EventBridge (YYYY-MM-DDTHH:MM:SS)
            String scheduleExpression = "at(" +
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                            .withZone(ZoneId.of("UTC"))
                            .format(scheduleTime) + ")";

            Target target = Target.builder()
                    .arn(sqsQueueArn)
                    .roleArn(schedulerRoleArn)
                    .input("{\"sessionId\":\"" + session.getId() + "\"}")
                    .build();

            CreateScheduleRequest request = CreateScheduleRequest.builder()
                    .name("session-onsale-" + session.getId().toString())
                    .groupName(schedulerGroupName)
                    .scheduleExpression(scheduleExpression)
                    .target(target)
                    .flexibleTimeWindow(FlexibleTimeWindow.builder().mode(FlexibleTimeWindowMode.OFF).build())
                    .actionAfterCompletion(ActionAfterCompletion.DELETE)
                    .build();


            schedulerClient.createSchedule(request);
            log.info("Successfully created EventBridge schedule for session {}", session.getId());
        } catch (Exception e) {
            // This is a critical failure. In a real system, you'd have a retry mechanism
            // or add it to a dead-letter queue for manual intervention.
            log.error("Failed to create EventBridge schedule for session {}", session.getId(), e);
        }
    }

    private EventResponseDTO mapToEventResponseDTO(Event event) {
        return EventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .status(event.getStatus().name())
                .organizationId(event.getOrganization().getId())
                .createdAt(event.getCreatedAt())
                .build();
    }
}