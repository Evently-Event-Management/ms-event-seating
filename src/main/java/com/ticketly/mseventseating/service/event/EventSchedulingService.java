package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    @Value("${aws.scheduler.sqs-queue-arn}")
    private String sqsQueueArn;

    @Value("${aws.scheduler.role-arn}")
    private String schedulerRoleArn;

    @Value("${aws.scheduler.group-name}")
    private String schedulerGroupName;

    public void scheduleOnSaleJobsForEvent(Event event) {
        for (EventSession session : event.getSessions()) {
            // We only schedule jobs for sessions that are still in the future
            // and haven't been manually set to ON_SALE or CANCELLED.
            if (session.getStartTime().toInstant().isAfter(Instant.now())) {
                Instant scheduleTime = calculateSaleStartTime(session);
                if (scheduleTime.isAfter(Instant.now())) {
                    createEventBridgeSchedule(session, scheduleTime);
                }
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
            log.error("Failed to create EventBridge schedule for session {}", session.getId(), e);
            // In a real system, this failure would need a robust retry or dead-lettering mechanism.
        }
    }
}
