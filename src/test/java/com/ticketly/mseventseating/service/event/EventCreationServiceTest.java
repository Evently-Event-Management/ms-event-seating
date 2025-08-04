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
import com.ticketly.mseventseating.service.S3StorageService;
import com.ticketly.mseventseating.service.SubscriptionTierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    @Mock
    private S3StorageService s3StorageService; // ✅ Mock the S3 service

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
        organizationId = UUID.randomUUID();
        userId = "user123";

        organization = Organization.builder().id(organizationId).name("Test Organization").build();
        event = Event.builder().id(UUID.randomUUID()).title("Test Event").status(EventStatus.PENDING).organization(organization).createdAt(OffsetDateTime.now()).build();

        List<SessionRequest> sessions = Collections.singletonList(
                SessionRequest.builder()
                        .startTime(OffsetDateTime.now().plusDays(10))
                        .endTime(OffsetDateTime.now().plusDays(10).plusHours(3))
                        .salesStartRuleType(SalesStartRuleType.FIXED)
                        .salesStartFixedDatetime(OffsetDateTime.now().plusDays(1))
                        .build()
        );

        createEventRequest = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Description")
                .organizationId(organizationId)
                .sessions(sessions)
                .build();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        jwt = Jwt.withTokenValue("token").header("alg", "RS256").claims(c -> c.putAll(claims)).build();

        // Use ReflectionTestUtils to set the values from properties
        ReflectionTestUtils.setField(eventCreationService, "maxCoverPhotos", 3);
        ReflectionTestUtils.setField(eventCreationService, "maxCoverPhotoSize", 5 * 1024 * 1024); // 5MB max size
    }

    @Test
    void createEvent_WithCoverImages_Success() throws IOException {
        // Arrange
        MockMultipartFile file1 = new MockMultipartFile("coverImages", "image1.jpg", "image/jpeg", "image1".getBytes());
        MultipartFile[] coverImages = {file1};
        List<String> s3Keys = List.of("s3-key-for-image1.jpg");

        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        // These are subscription tier limits, not file size limits
        when(tierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(tierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(10);
        when(s3StorageService.uploadFile(any(MultipartFile.class), eq("event-cover-photos"))).thenReturn("s3-key-for-image1.jpg");
        when(eventFactory.createFromRequest(eq(createEventRequest), eq(organization), eq(s3Keys))).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);

        // Act
        EventResponseDTO response = eventCreationService.createEvent(createEventRequest, coverImages, userId, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(event.getId(), response.getId());

        // Verify interactions
        verify(s3StorageService, times(1)).uploadFile(file1, "event-cover-photos");
        verify(eventFactory).createFromRequest(createEventRequest, organization, s3Keys);
        verify(eventRepository).save(event);
    }

    @Test
    void createEvent_WithNoCoverImages_Success() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(tierService.getLimit(any(), any())).thenReturn(10);
        when(eventFactory.createFromRequest(eq(createEventRequest), eq(organization), eq(Collections.emptyList()))).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);

        // Act
        EventResponseDTO response = eventCreationService.createEvent(createEventRequest, null, userId, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(event.getId(), response.getId());

        // Verify S3 service was never called
        verifyNoInteractions(s3StorageService);
        // Verify factory was called with an empty list of keys
        verify(eventFactory).createFromRequest(createEventRequest, organization, Collections.emptyList());
    }

    @Test
    void createEvent_ExceedsActiveEventLimit_ThrowsBadRequestException() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(tierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                eventCreationService.createEvent(createEventRequest, null, userId, jwt));

        assertTrue(exception.getMessage().contains("You have reached the limit of 5 active events"));
        verifyNoInteractions(eventFactory, s3StorageService);
    }

    @Test
    void createEvent_ExceedsCoverPhotoLimit_ThrowsBadRequestException() {
        // Arrange
        MultipartFile[] tooManyFiles = {
                new MockMultipartFile("file", "1.jpg", "image/jpeg", "1".getBytes()),
                new MockMultipartFile("file", "2.jpg", "image/jpeg", "2".getBytes()),
                new MockMultipartFile("file", "3.jpg", "image/jpeg", "3".getBytes()),
                new MockMultipartFile("file", "4.jpg", "image/jpeg", "4".getBytes())
        }; // 4 files, but limit is 3

        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(tierService.getLimit(any(), any())).thenReturn(10);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                eventCreationService.createEvent(createEventRequest, tooManyFiles, userId, jwt));

        assertTrue(exception.getMessage().contains("You can upload a maximum of 3 cover photos"));
        verifyNoInteractions(eventFactory, s3StorageService);
    }

    @Test
    void createEvent_ExceedsCoverPhotoSizeLimit_ThrowsBadRequestException() {
        // Arrange
        // Create a large file that exceeds the 5MB limit set in setUp
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB, exceeding the 5MB limit
        Arrays.fill(largeContent, (byte) 1);

        MockMultipartFile largeFile = new MockMultipartFile(
                "coverImages",
                "large_image.jpg",
                "image/jpeg",
                largeContent
        );

        MultipartFile[] coverImages = {largeFile};

        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(tierService.getLimit(any(), any())).thenReturn(10); // Tier limits are not exceeded

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                eventCreationService.createEvent(createEventRequest, coverImages, userId, jwt));

        assertTrue(exception.getMessage().contains("File size exceeds the maximum allowed size of 5MB"));
        verifyNoInteractions(eventFactory);
        verifyNoInteractions(s3StorageService);
    }
}
