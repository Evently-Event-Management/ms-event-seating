package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.exception.SchedulingException;
import com.ticketly.mseventseating.model.Event;
import com.ticketly.mseventseating.model.EventSession;
import model.SessionStatus;
import model.SessionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSchedulingServiceTest {

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private EventSchedulingService eventSchedulingService;

    private Event event;
    private EventSession futureSession;
    private EventSession pastSession;
    private EventSession cancelledSession;
    private EventSession soldOutSession;
    private EventSession pendingSessionWithoutStartTime;
    private EventSession pendingSessionWithoutEndTime;

    private final String sqsOnSaleQueueArn = "arn:aws:sqs:region:account:on-sale-queue";
    private final String sqsClosedQueueArn = "arn:aws:sqs:region:account:closed-queue";
    private final String schedulerRoleArn = "arn:aws:iam:region:account:role/scheduler-role";
    private final String schedulerGroupName = "test-scheduler-group";

    @BeforeEach
    void setUp() {
        // Set up field values using ReflectionTestUtils
        ReflectionTestUtils.setField(eventSchedulingService, "sqsOnSaleQueueArn", sqsOnSaleQueueArn);
        ReflectionTestUtils.setField(eventSchedulingService, "sqsClosedQueueArn", sqsClosedQueueArn);
        ReflectionTestUtils.setField(eventSchedulingService, "schedulerRoleArn", schedulerRoleArn);
        ReflectionTestUtils.setField(eventSchedulingService, "schedulerGroupName", schedulerGroupName);

        // Set up event
        event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Test Event");

        // Set up future session (start time in the future)
        futureSession = new EventSession();
        futureSession.setId(UUID.randomUUID());
        futureSession.setStatus(SessionStatus.PENDING);
        futureSession.setSessionType(SessionType.PHYSICAL);
        futureSession.setStartTime(OffsetDateTime.now().plusDays(7));
        futureSession.setEndTime(OffsetDateTime.now().plusDays(7).plusHours(2));
        futureSession.setSalesStartTime(OffsetDateTime.now().plusDays(1));
        futureSession.setEvent(event);

        // Set up past session (start time in the past)
        pastSession = new EventSession();
        pastSession.setId(UUID.randomUUID());
        pastSession.setStatus(SessionStatus.PENDING);
        pastSession.setSessionType(SessionType.PHYSICAL);
        pastSession.setStartTime(OffsetDateTime.now().minusDays(1));
        pastSession.setEndTime(OffsetDateTime.now().minusDays(1).plusHours(2));
        pastSession.setSalesStartTime(OffsetDateTime.now().minusDays(2));
        pastSession.setEvent(event);

        // Set up cancelled session
        cancelledSession = new EventSession();
        cancelledSession.setId(UUID.randomUUID());
        cancelledSession.setStatus(SessionStatus.CANCELLED);
        cancelledSession.setSessionType(SessionType.PHYSICAL);
        cancelledSession.setStartTime(OffsetDateTime.now().plusDays(7));
        cancelledSession.setEndTime(OffsetDateTime.now().plusDays(7).plusHours(2));
        cancelledSession.setSalesStartTime(OffsetDateTime.now().plusDays(1));
        cancelledSession.setEvent(event);

        // Set up sold out session
        soldOutSession = new EventSession();
        soldOutSession.setId(UUID.randomUUID());
        soldOutSession.setStatus(SessionStatus.SOLD_OUT);
        soldOutSession.setSessionType(SessionType.PHYSICAL);
        soldOutSession.setStartTime(OffsetDateTime.now().plusDays(7));
        soldOutSession.setEndTime(OffsetDateTime.now().plusDays(7).plusHours(2));
        soldOutSession.setSalesStartTime(OffsetDateTime.now().plusDays(1));
        soldOutSession.setEvent(event);

        // Set up pending session without start time
        pendingSessionWithoutStartTime = new EventSession();
        pendingSessionWithoutStartTime.setId(UUID.randomUUID());
        pendingSessionWithoutStartTime.setStatus(SessionStatus.PENDING);
        pendingSessionWithoutStartTime.setSessionType(SessionType.PHYSICAL);
        pendingSessionWithoutStartTime.setStartTime(null);
        pendingSessionWithoutStartTime.setEndTime(OffsetDateTime.now().plusDays(7).plusHours(2));
        pendingSessionWithoutStartTime.setSalesStartTime(OffsetDateTime.now().plusDays(1));
        pendingSessionWithoutStartTime.setEvent(event);

        // Set up pending session without end time
        pendingSessionWithoutEndTime = new EventSession();
        pendingSessionWithoutEndTime.setId(UUID.randomUUID());
        pendingSessionWithoutEndTime.setStatus(SessionStatus.PENDING);
        pendingSessionWithoutEndTime.setSessionType(SessionType.PHYSICAL);
        pendingSessionWithoutEndTime.setStartTime(OffsetDateTime.now().plusDays(7));
        pendingSessionWithoutEndTime.setEndTime(null);
        pendingSessionWithoutEndTime.setSalesStartTime(OffsetDateTime.now().plusDays(1));
        pendingSessionWithoutEndTime.setEvent(event);
    }

    @Test
    @DisplayName("Should skip scheduling when event has no sessions")
    void scheduleOnSaleJobsForEvent_whenNoSessions_shouldSkipScheduling() {
        // Arrange
        Event eventWithNoSessions = new Event();
        eventWithNoSessions.setId(UUID.randomUUID());
        eventWithNoSessions.setTitle("Event with no sessions");
        eventWithNoSessions.setSessions(new ArrayList<>());

        // Act
        eventSchedulingService.scheduleOnSaleJobsForEvent(eventWithNoSessions);

        // Assert
        verify(schedulerClient, never()).createSchedule(any(CreateScheduleRequest.class));
        verify(schedulerClient, never()).updateSchedule(any(UpdateScheduleRequest.class));
    }

    @Test
    @DisplayName("Should skip sessions with status CANCELLED, SOLD_OUT, or CLOSED")
    void scheduleOnSaleJobsForEvent_whenSessionsAreNotPending_shouldSkipThoseSessions() {
        // Arrange
        event.setSessions(Arrays.asList(
                cancelledSession,
                soldOutSession,
                futureSession
        ));

        // Act
        eventSchedulingService.scheduleOnSaleJobsForEvent(event);

        // Assert - should only process the future session (not cancelled or sold out)
        verify(schedulerClient, times(2)).createSchedule(any(CreateScheduleRequest.class));
    }

    @Test
    @DisplayName("Should skip sessions with null start time")
    void scheduleOnSaleJobsForEvent_whenSessionHasNullStartTime_shouldSkipSession() {
        // Arrange
        event.setSessions(Arrays.asList(
                pendingSessionWithoutStartTime,
                futureSession
        ));

        // Act
        eventSchedulingService.scheduleOnSaleJobsForEvent(event);

        // Assert - should only process the future session
        verify(schedulerClient, times(2)).createSchedule(any(CreateScheduleRequest.class));
    }

    @Test
    @DisplayName("Should skip sessions that have already started or are in the past")
    void scheduleOnSaleJobsForEvent_whenSessionIsInPast_shouldSkipScheduling() {
        // Arrange
        event.setSessions(Arrays.asList(
                pastSession,
                futureSession
        ));

        // Act
        eventSchedulingService.scheduleOnSaleJobsForEvent(event);

        // Assert - should only process the future session
        verify(schedulerClient, times(2)).createSchedule(any(CreateScheduleRequest.class));
    }

    @Test
    @DisplayName("Should create on-sale and closed schedules for future sessions")
    void scheduleOnSaleJobsForEvent_withFutureSession_shouldCreateOnSaleAndClosedSchedules() {
        // Arrange
        event.setSessions(List.of(futureSession));

        // Capture the create schedule requests
        ArgumentCaptor<CreateScheduleRequest> createRequestCaptor = ArgumentCaptor.forClass(CreateScheduleRequest.class);

        // Act
        eventSchedulingService.scheduleOnSaleJobsForEvent(event);

        // Assert
        verify(schedulerClient, times(2)).createSchedule(createRequestCaptor.capture());

        List<CreateScheduleRequest> capturedRequests = createRequestCaptor.getAllValues();
        assertEquals(2, capturedRequests.size());

        // Verify on-sale schedule
        CreateScheduleRequest onSaleRequest = capturedRequests.stream()
                .filter(req -> req.name().startsWith("session-onsale-"))
                .findFirst()
                .orElseThrow();

        assertEquals("session-onsale-" + futureSession.getId().toString(), onSaleRequest.name());
        assertEquals(schedulerGroupName, onSaleRequest.groupName());
        assertTrue(onSaleRequest.target().input().contains("\"action\":\"ON_SALE\""));
        assertEquals(sqsOnSaleQueueArn, onSaleRequest.target().arn());
        assertEquals(schedulerRoleArn, onSaleRequest.target().roleArn());

        // Verify closed schedule
        CreateScheduleRequest closedRequest = capturedRequests.stream()
                .filter(req -> req.name().startsWith("session-closed-"))
                .findFirst()
                .orElseThrow();

        assertEquals("session-closed-" + futureSession.getId().toString(), closedRequest.name());
        assertEquals(schedulerGroupName, closedRequest.groupName());
        assertTrue(closedRequest.target().input().contains("\"action\":\"CLOSED\""));
        assertEquals(sqsClosedQueueArn, closedRequest.target().arn());
        assertEquals(schedulerRoleArn, closedRequest.target().roleArn());
    }

    @Test
    @DisplayName("Should use current time if sales start time is in the past")
    void scheduleOnSaleJobsForEvent_whenSalesStartTimeInPast_shouldUseCurrentTime() {
        // Arrange
        EventSession sessionWithPastSalesStart = new EventSession();
        sessionWithPastSalesStart.setId(UUID.randomUUID());
        sessionWithPastSalesStart.setStatus(SessionStatus.PENDING);
        sessionWithPastSalesStart.setSessionType(SessionType.PHYSICAL);
        sessionWithPastSalesStart.setStartTime(OffsetDateTime.now().plusDays(7));
        sessionWithPastSalesStart.setEndTime(OffsetDateTime.now().plusDays(7).plusHours(2));
        sessionWithPastSalesStart.setSalesStartTime(OffsetDateTime.now().minusDays(1)); // Sales start in the past
        sessionWithPastSalesStart.setEvent(event);

        event.setSessions(List.of(sessionWithPastSalesStart));

        ArgumentCaptor<CreateScheduleRequest> createRequestCaptor = ArgumentCaptor.forClass(CreateScheduleRequest.class);

        // Get current time for comparison
        Instant beforeTest = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // Act
        eventSchedulingService.scheduleOnSaleJobsForEvent(event);

        Instant afterTest = Instant.now().plusSeconds(10).truncatedTo(ChronoUnit.SECONDS);

        // Assert
        verify(schedulerClient, times(2)).createSchedule(createRequestCaptor.capture());

        // Get the on-sale request
        CreateScheduleRequest onSaleRequest = createRequestCaptor.getAllValues().stream()
                .filter(req -> req.name().startsWith("session-onsale-"))
                .findFirst()
                .orElseThrow();

        // Extract schedule time from expression (format: at(yyyy-MM-dd'T'HH:mm:ss))
        String scheduleExpression = onSaleRequest.scheduleExpression();
        String timeStr = scheduleExpression.substring(3, scheduleExpression.length() - 1);
        Instant scheduleTime = OffsetDateTime.parse(timeStr + "Z").toInstant();

        // Schedule time should be close to current time
        assertTrue(scheduleTime.isAfter(beforeTest) || scheduleTime.equals(beforeTest));
        assertTrue(scheduleTime.isBefore(afterTest) || scheduleTime.equals(afterTest));
    }

    @Test
    @DisplayName("Should not schedule closed job if session has no end time")
    void scheduleOnSaleJobsForEvent_whenSessionHasNoEndTime_shouldNotScheduleClosedJob() {
        // Arrange
        event.setSessions(List.of(pendingSessionWithoutEndTime));

        // Act
        eventSchedulingService.scheduleOnSaleJobsForEvent(event);

        // Assert - should only create on-sale schedule, not closed
        ArgumentCaptor<CreateScheduleRequest> createRequestCaptor = ArgumentCaptor.forClass(CreateScheduleRequest.class);
        verify(schedulerClient, times(1)).createSchedule(createRequestCaptor.capture());

        CreateScheduleRequest request = createRequestCaptor.getValue();
        assertEquals("session-onsale-" + pendingSessionWithoutEndTime.getId().toString(), request.name());
    }

    @Test
    @DisplayName("Should update schedule if it already exists")
    void scheduleOnSaleJobsForEvent_whenScheduleExists_shouldUpdateSchedule() {
        // Arrange
        event.setSessions(List.of(futureSession));

        // Mock a conflict exception for the first schedule creation attempt
        // This is the corrected line
        doThrow(ConflictException.class)
                .when(schedulerClient)
                .createSchedule(argThat((CreateScheduleRequest request) ->
                        request.name() != null && request.name().startsWith("session-onsale-")));

        ArgumentCaptor<UpdateScheduleRequest> updateRequestCaptor = ArgumentCaptor.forClass(UpdateScheduleRequest.class);

        // Act
        eventSchedulingService.scheduleOnSaleJobsForEvent(event);

        // Assert
        verify(schedulerClient).updateSchedule(updateRequestCaptor.capture());

        UpdateScheduleRequest updateRequest = updateRequestCaptor.getValue();
        assertEquals("session-onsale-" + futureSession.getId().toString(), updateRequest.name());
        assertEquals(schedulerGroupName, updateRequest.groupName());
        assertTrue(updateRequest.target().input().contains("\"action\":\"ON_SALE\""));
        assertEquals(sqsOnSaleQueueArn, updateRequest.target().arn());
    }

    @Test
    @DisplayName("Should throw SchedulingException when there are scheduling errors")
    void scheduleOnSaleJobsForEvent_whenSchedulingFails_shouldThrowSchedulingException() {
        // Arrange
        event.setSessions(List.of(futureSession));

        // Mock a generic exception for scheduling attempt
        doThrow(new RuntimeException("Scheduling failed"))
                .when(schedulerClient)
                .createSchedule(any(CreateScheduleRequest.class));

        // Act & Assert
        SchedulingException exception = assertThrows(SchedulingException.class, () ->
                eventSchedulingService.scheduleOnSaleJobsForEvent(event));

        assertTrue(exception.getMessage().contains("Failed to schedule one or more sessions"));
        assertNotNull(exception.getCause());
        assertEquals("Scheduling failed", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle multiple sessions correctly")
    void scheduleOnSaleJobsForEvent_withMultipleSessions_shouldProcessAllEligibleSessions() {
        // Arrange
        // Create a second future session
        EventSession anotherFutureSession = new EventSession();
        anotherFutureSession.setId(UUID.randomUUID());
        anotherFutureSession.setStatus(SessionStatus.PENDING);
        anotherFutureSession.setSessionType(SessionType.ONLINE);
        anotherFutureSession.setStartTime(OffsetDateTime.now().plusDays(14));
        anotherFutureSession.setEndTime(OffsetDateTime.now().plusDays(14).plusHours(2));
        anotherFutureSession.setSalesStartTime(OffsetDateTime.now().plusDays(7));
        anotherFutureSession.setEvent(event);

        event.setSessions(Arrays.asList(
                futureSession,
                anotherFutureSession,
                cancelledSession,  // Should be skipped
                pastSession,       // Should be skipped
                soldOutSession     // Should be skipped
        ));

        // Act
        eventSchedulingService.scheduleOnSaleJobsForEvent(event);

        // Assert
        // For each future session: 1 for on-sale + 1 for closed = 2 per session
        // We have 2 valid future sessions, so 2*2 = 4 create requests
        verify(schedulerClient, times(4)).createSchedule(any(CreateScheduleRequest.class));
    }
}
