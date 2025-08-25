package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.SessionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSchedulingService {

    private final SchedulerClient schedulerClient;

    @Value("${aws.scheduler.sqs-session-on-sale-queue-arn}")
    private String sqsOnSaleQueueArn;

    @Value("${aws.scheduler.sqs-session-closed-queue-arn}")
    private String sqsClosedQueueArn;

    @Value("${aws.scheduler.role-arn}")
    private String schedulerRoleArn;

    @Value("${aws.scheduler.group-name}")
    private String schedulerGroupName;

    /**
     * Schedules on-sale jobs for all valid sessions of an event.
     * This operation should be part of a transaction when the event status changes.
     */
    @Transactional
    public void scheduleOnSaleJobsForEvent(Event event) {
        log.debug("Starting to schedule on-sale jobs for event ID: {}, title: {}", event.getId(), event.getTitle());

        if (event.getSessions() == null || event.getSessions().isEmpty()) {
            log.debug("No sessions found for event ID: {}, skipping scheduling", event.getId());
            return;
        }

        log.debug("Found {} sessions to process for event ID: {}", event.getSessions().size(), event.getId());

        for (EventSession session : event.getSessions()) {
            // Skip already cancelled sessions
            if (session.getStatus() != SessionStatus.PENDING) {
                log.debug("Skipping session ID: {} with status: {}", session.getId(), session.getStatus());
                continue;
            }

            log.debug("Processing session ID: {}, start time: {}", session.getId(), session.getStartTime());

            if (session.getStartTime() == null) {
                log.debug("Session ID: {} has null start time, skipping", session.getId());
                continue;
            }

            if (session.getStartTime().toInstant().isAfter(Instant.now())) {
                log.debug("Session ID: {} is in the future, calculating sale start time", session.getId());
                Instant scheduleTime = calculateSaleStartTime(session);

                log.debug("Calculated sale start time for session ID: {} is: {}", session.getId(), scheduleTime);

                // For future sessions, we schedule regardless of whether the calculated start time is in the past
                // If start time is in the past, we'll need to make tickets available immediately
                createEventBridgeSchedule(session, scheduleTime.isAfter(Instant.now()) ? scheduleTime : Instant.now());

                // Also schedule a job to mark the session as SOLD_OUT once it ends
                if (session.getEndTime() != null) {
                    scheduleSessionClosedJob(session);
                }
            } else {
                log.debug("Session ID: {} has already started or is in the past, skipping", session.getId());
            }
        }

        log.debug("Completed scheduling on-sale jobs for event ID: {}", event.getId());
    }

    /**
     * Schedules a job to mark a session as CLOSED once it ends
     */
    private void scheduleSessionClosedJob(EventSession session) {
        if (session.getEndTime() == null) {
            log.debug("Session ID: {} has null end time, skipping closed scheduling", session.getId());
            return;
        }

        Instant endTime = session.getEndTime().toInstant();
        log.debug("Scheduling closed job for session ID: {} at end time: {}", session.getId(), endTime);

        try {
            String scheduleName = "session-closed-" + session.getId().toString();

            String scheduleExpression = "at(" +
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                            .withZone(ZoneId.of("UTC"))
                            .format(endTime) + ")";

            log.debug("Schedule expression for closed job for session ID: {}: {}", session.getId(), scheduleExpression);

            String inputJson = "{\"sessionId\":\"" + session.getId() + "\", \"action\":\"CLOSED\"}";
            log.debug("Schedule payload for closed job for session ID: {}: {}", session.getId(), inputJson);

            Target target = Target.builder()
                    .arn(sqsClosedQueueArn)
                    .roleArn(schedulerRoleArn)
                    .input(inputJson)
                    .build();

            CreateScheduleRequest request = CreateScheduleRequest.builder()
                    .name(scheduleName)
                    .groupName(schedulerGroupName)
                    .scheduleExpression(scheduleExpression)
                    .target(target)
                    .flexibleTimeWindow(FlexibleTimeWindow.builder().mode(FlexibleTimeWindowMode.OFF).build())
                    .actionAfterCompletion(ActionAfterCompletion.DELETE)
                    .build();

            log.debug("Sending CreateSchedule request for closed job for session ID: {}", session.getId());
            schedulerClient.createSchedule(request);
            log.info("Successfully created EventBridge schedule for closed job for session {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to create EventBridge schedule for closed job for session {}", session.getId(), e);
        }
    }

    private Instant calculateSaleStartTime(EventSession session) {
        log.debug("Calculating sale start time for session ID: {} with rule type: {}",
                session.getId(), session.getSalesStartRuleType());

        Instant result = switch (session.getSalesStartRuleType()) {
            case IMMEDIATE -> {
                log.debug("Using IMMEDIATE rule type for session ID: {}", session.getId());
                yield Instant.now();
            }
            case FIXED -> {
                if (session.getSalesStartFixedDatetime() == null) {
                    log.debug("Fixed sale start time is null for session ID: {}, defaulting to now", session.getId());
                    yield Instant.now();
                }
                log.debug("Using FIXED rule type for session ID: {}, time: {}",
                        session.getId(), session.getSalesStartFixedDatetime());
                yield session.getSalesStartFixedDatetime().toInstant();
            }
            case ROLLING -> {
                log.debug("Using ROLLING rule type for session ID: {}, hours before: {}",
                        session.getId(), session.getSalesStartHoursBefore());

                if (session.getSalesStartHoursBefore() == null) {
                    log.debug("Sales start hours is null for ROLLING rule in session ID: {}, throwing exception", session.getId());
                    throw new BadRequestException("Sales start hours must be provided for rolling sales rule.");
                }

                Instant calculatedTime = session.getStartTime().minusHours(session.getSalesStartHoursBefore()).toInstant();
                log.debug("Calculated ROLLING sale time for session ID: {} is: {}", session.getId(), calculatedTime);
                yield calculatedTime;
            }
        };

        log.debug("Final sale start time for session ID: {} is: {}", session.getId(), result);
        return result;
    }

    private void createEventBridgeSchedule(EventSession session, Instant scheduleTime) {
        String scheduleName = "session-onsale-" + session.getId().toString();
        log.debug("Creating EventBridge schedule '{}' for session ID: {} at time: {}",
                scheduleName, session.getId(), scheduleTime);

        try {
            String scheduleExpression = "at(" +
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                            .withZone(ZoneId.of("UTC"))
                            .format(scheduleTime) + ")";

            log.debug("Schedule expression for session ID: {}: {}", session.getId(), scheduleExpression);
            log.debug("Using SQS queue ARN: {}", sqsOnSaleQueueArn);
            log.debug("Using scheduler role ARN: {}", schedulerRoleArn);

            String inputJson = "{\"sessionId\":\"" + session.getId() + "\", \"action\":\"ON_SALE\"}";
            log.debug("Schedule payload for session ID: {}: {}", session.getId(), inputJson);

            Target target = Target.builder()
                    .arn(sqsOnSaleQueueArn)
                    .roleArn(schedulerRoleArn)
                    .input(inputJson)
                    .build();

            CreateScheduleRequest request = CreateScheduleRequest.builder()
                    .name(scheduleName)
                    .groupName(schedulerGroupName)
                    .scheduleExpression(scheduleExpression)
                    .target(target)
                    .flexibleTimeWindow(FlexibleTimeWindow.builder().mode(FlexibleTimeWindowMode.OFF).build())
                    .actionAfterCompletion(ActionAfterCompletion.DELETE)
                    .build();

            log.debug("Sending CreateSchedule request for session ID: {}", session.getId());
            schedulerClient.createSchedule(request);
            log.info("Successfully created EventBridge schedule for session {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to create EventBridge schedule for session {}", session.getId(), e);
            // In a real system, this failure would need a robust retry or dead-lettering mechanism.
        }
    }
}
