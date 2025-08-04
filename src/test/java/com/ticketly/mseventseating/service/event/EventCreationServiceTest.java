package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.dto.session.SessionRequest;
import com.ticketly.mseventseating.model.SalesStartRuleType;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.factory.EventFactory;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.OrganizationOwnershipService;
import com.ticketly.mseventseating.service.SubscriptionTierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCreationServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizationOwnershipService ownershipService;

    @Mock
    private SubscriptionTierService tierService;

    @Mock
    private EventFactory eventFactory;

    @InjectMocks
    private EventCreationService eventCreationService;

    private UUID organizationId;
    private String userId;
    private Organization organization;
    private Event event;
    private CreateEventRequest createEventRequest;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        // Initialize common test data
        organizationId = UUID.randomUUID();
        userId = "user123";

        // Setup organization
        organization = Organization.builder()
                .id(organizationId)
                .name("Test Organization")
                .build();

        // Setup event
        event = Event.builder()
                .id(UUID.randomUUID())
                .title("Test Event")
                .description("Test Description")
                .overview("Test Overview")
                .status(EventStatus.PENDING)
                .organization(organization)
                .createdAt(OffsetDateTime.now())
                .build();

        // Setup sessions for the request
        List<SessionRequest> sessions = Collections.singletonList(
                SessionRequest.builder()
                        .startTime(OffsetDateTime.now().plusDays(10))
                        .endTime(OffsetDateTime.now().plusDays(10).plusHours(3))
                        .salesStartRuleType(SalesStartRuleType.FIXED)
                        .salesStartFixedDatetime(OffsetDateTime.now().plusDays(1))
                        .build()
        );

        // Setup create event request
        createEventRequest = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Description")
                .overview("Test Overview")
                .organizationId(organizationId)
                .sessions(sessions)
                .build();

        // Setup JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);

        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void createEvent_Success() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(eventFactory.createFromRequest(eq(createEventRequest), eq(organization))).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        when(tierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(tierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);

        // Act
        EventResponseDTO response = eventCreationService.createEvent(createEventRequest, userId, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(event.getId(), response.getId());
        assertEquals(event.getTitle(), response.getTitle());
        assertEquals(event.getStatus().name(), response.getStatus());
        assertEquals(organization.getId(), response.getOrganizationId());

        // Verify
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(tierService).getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        verify(tierService).getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        verify(eventFactory).createFromRequest(createEventRequest, organization);
        verify(eventRepository).save(event);
        verify(eventRepository).countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
    }

    @Test
    void createEvent_ExceedsActiveEventLimit_ThrowsBadRequestException() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(tierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            eventCreationService.createEvent(createEventRequest, userId, jwt));

        assertTrue(exception.getMessage().contains("You have reached the limit of 5 active events"));

        // Verify
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(tierService).getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        verify(eventRepository).countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
        verifyNoInteractions(eventFactory);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_ExceedsSessionsPerEventLimit_ThrowsBadRequestException() {
        // Arrange
        // Create a request with too many sessions
        List<SessionRequest> manySessions = Arrays.asList(
                SessionRequest.builder().startTime(OffsetDateTime.now().plusDays(1)).endTime(OffsetDateTime.now().plusDays(1).plusHours(2)).build(),
                SessionRequest.builder().startTime(OffsetDateTime.now().plusDays(2)).endTime(OffsetDateTime.now().plusDays(2).plusHours(2)).build(),
                SessionRequest.builder().startTime(OffsetDateTime.now().plusDays(3)).endTime(OffsetDateTime.now().plusDays(3).plusHours(2)).build()
        );

        CreateEventRequest requestWithManySessions = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Description")
                .organizationId(organizationId)
                .sessions(manySessions)
                .build();

        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(tierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(tierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(2); // Limit is 2, but we're trying to create 3
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            eventCreationService.createEvent(requestWithManySessions, userId, jwt));

        assertTrue(exception.getMessage().contains("You cannot create more than 2 sessions per event"));

        // Verify
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(tierService).getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        verify(tierService).getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        verify(eventRepository).countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
        verifyNoInteractions(eventFactory);
        verify(eventRepository, never()).save(any());
    }
}
