package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.SchedulingException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.SessionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void scheduleOnSaleJobsForEvent(Event event) {
        log.debug("Starting to schedule on-sale jobs for event ID: {}, title: {}", event.getId(), event.getTitle());
        List<Exception> schedulingErrors = new ArrayList<>();

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

                try {
                    // If the sales start time is in the past, make tickets available immediately
                    createEventBridgeSchedule(session, scheduleTime.isAfter(Instant.now()) ? scheduleTime : Instant.now());

                    // Also schedule a job to mark the session as CLOSED once it ends
                    if (session.getEndTime() != null) {
                        scheduleSessionClosedJob(session);
                    }
                } catch (Exception e) {
                    schedulingErrors.add(e);
                }
            } else {
                log.debug("Session ID: {} has already started or is in the past, skipping", session.getId());
            }
        }

        // If there were any errors during scheduling, throw an exception to roll back the transaction
        if (!schedulingErrors.isEmpty()) {
            String errorMessage = String.format("Failed to schedule one or more sessions for event %s. %d error(s) occurred.",
                    event.getId(), schedulingErrors.size());
            log.error(errorMessage);
            throw new SchedulingException(errorMessage, schedulingErrors.getFirst());
        }

        log.debug("Completed scheduling on-sale jobs for event ID: {}", event.getId());
    }

    // REFACTORED METHOD
    private void scheduleSessionClosedJob(EventSession session) {
        if (session.getEndTime() == null) {
            log.debug("Session ID: {} has null end time, skipping closed scheduling", session.getId());
            return;
        }
        createOrUpdateSchedule(session, session.getEndTime().toInstant(), "session-closed-", sqsClosedQueueArn, "CLOSED", "closed job");
    }

    // REFACTORED METHOD
    private void createEventBridgeSchedule(EventSession session, Instant scheduleTime) {
        createOrUpdateSchedule(session, scheduleTime, "session-onsale-", sqsOnSaleQueueArn, "ON_SALE", "on-sale job");
    }

    /**
     * NEW private helper method that contains the shared logic for creating or updating a schedule.
     */
    private void createOrUpdateSchedule(EventSession session, Instant scheduleTime, String namePrefix, String queueArn, String action, String logContext) {
        String scheduleName = namePrefix + session.getId().toString();
        log.debug("Creating or updating schedule '{}' for session ID: {} at time: {}", scheduleName, session.getId(), scheduleTime);

        try {
            String scheduleExpression = "at(" +
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                            .withZone(ZoneId.of("UTC"))
                            .format(scheduleTime) + ")";

            String inputJson = "{\"sessionId\":\"" + session.getId() + "\", \"action\":\"" + action + "\"}";

            Target target = Target.builder()
                    .arn(queueArn)
                    .roleArn(schedulerRoleArn)
                    .input(inputJson)
                    .build();

            FlexibleTimeWindow flexibleTimeWindow = FlexibleTimeWindow.builder().mode(FlexibleTimeWindowMode.OFF).build();

            try {
                // First, try to create the schedule
                CreateScheduleRequest request = CreateScheduleRequest.builder()
                        .name(scheduleName)
                        .groupName(schedulerGroupName)
                        .scheduleExpression(scheduleExpression)
                        .target(target)
                        .flexibleTimeWindow(flexibleTimeWindow)
                        .actionAfterCompletion(ActionAfterCompletion.DELETE)
                        .build();

                schedulerClient.createSchedule(request);
                log.info("Successfully created EventBridge schedule for {} for session {}", logContext, session.getId());

            } catch (ConflictException e) {
                // If it already exists, update it instead
                log.warn("Schedule '{}' already exists. Attempting to update.", scheduleName);
                UpdateScheduleRequest updateRequest = UpdateScheduleRequest.builder()
                        .name(scheduleName)
                        .groupName(schedulerGroupName)
                        .scheduleExpression(scheduleExpression)
                        .target(target)
                        .flexibleTimeWindow(flexibleTimeWindow)
                        .actionAfterCompletion(ActionAfterCompletion.DELETE)
                        .build();

                schedulerClient.updateSchedule(updateRequest);
                log.info("Successfully updated EventBridge schedule for {} for session {}", logContext, session.getId());
            }

        } catch (Exception e) {
            log.error("Failed to create or update EventBridge schedule for {} for session {}", logContext, session.getId(), e);
            throw e;
        }
    }
}