package com.ticketly.mseventseating.service.event;

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
            // Skip non-pending sessions
            if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.SOLD_OUT || session.getStatus() == SessionStatus.CLOSED) {
                log.debug("Skipping session ID: {} with status: {}", session.getId(), session.getStatus());
                continue;
            }

            log.debug("Processing session ID: {}, start time: {}", session.getId(), session.getStartTime());

            if (session.getStartTime() == null) {
                log.debug("Session ID: {} has null start time, skipping", session.getId());
                continue;
            }

            if (session.getStartTime().toInstant().isAfter(Instant.now())) {
                log.debug("Session ID: {} is in the future, using provided sale start time", session.getId());

                // Use the sales start time directly provided by the frontend
                Instant scheduleTime = session.getSalesStartTime() != null ?
                    session.getSalesStartTime().toInstant() : Instant.now();

                log.debug("Using sales start time for session ID: {} is: {}", session.getId(), scheduleTime);

                // If the sales start time is in the past, make tickets available immediately
                createEventBridgeSchedule(session, scheduleTime.isAfter(Instant.now()) ? scheduleTime : Instant.now());

                // Also schedule a job to mark the session as CLOSED once it ends
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
