package com.ticketly.mseventseating.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.EventDetailDTO;
import com.ticketly.mseventseating.dto.event.EventSummaryDTO;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventOwnershipService eventOwnershipService;

    @InjectMocks
    private EventQueryService eventQueryService;

    private UUID eventId;
    private UUID organizationId;
    private String userId;
    private Event event;
    private Pageable pageable;
    private List<Event> eventList;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        userId = "test-user-id";
        pageable = PageRequest.of(0, 10);

        // Setup Organization
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setUserId(userId);
        organization.setName("Test Organization");

        // Setup Category
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Test Category");

        // Setup Event
        event = new Event();
        event.setId(eventId);
        event.setTitle("Test Event");
        event.setDescription("Test Description");
        event.setOverview("Test Overview");
        event.setOrganization(organization);
        event.setCategory(category);
        event.setStatus(EventStatus.PENDING);
        event.setCoverPhotos(Arrays.asList("photo1.jpg", "photo2.jpg"));
        event.setCreatedAt(OffsetDateTime.now().minusDays(1));
        event.setUpdatedAt(OffsetDateTime.now());
        
        // Setup Tiers
        Tier tier = new Tier();
        tier.setId(UUID.randomUUID());
        tier.setName("VIP");
        tier.setColor("#FF0000");
        tier.setPrice(BigDecimal.valueOf(100.0));
        tier.setEvent(event);
        
        // Setup Sessions
        EventSession session = new EventSession();
        session.setId(UUID.randomUUID());
        session.setStartTime(OffsetDateTime.now().plusDays(1));
        session.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(2));
        session.setEvent(event);
        session.setStatus(SessionStatus.PENDING);
        
        event.setTiers(Collections.singletonList(tier));
        event.setSessions(Collections.singletonList(session));
        
        // Setup event list for pagination tests
        eventList = new ArrayList<>();
        eventList.add(event);
        
        // Add another event
        Event event2 = new Event();
        event2.setId(UUID.randomUUID());
        event2.setTitle("Second Test Event");
        event2.setDescription("Second Test Description");
        event2.setOrganization(organization);
        event2.setCategory(category);
        event2.setStatus(EventStatus.APPROVED);
        event2.setCoverPhotos(Collections.singletonList("photo3.jpg"));
        event2.setTiers(Collections.emptyList());
        event2.setSessions(Collections.emptyList());
        eventList.add(event2);
    }

    @Test
    @DisplayName("Should return all events when status is null")
    void findAllEvents_whenStatusIsNull_shouldReturnAllEvents() {
        // Arrange
        Page<Event> eventPage = new PageImpl<>(eventList, pageable, eventList.size());
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("Test Event", result.getContent().get(0).getTitle());
        assertEquals("Second Test Event", result.getContent().get(1).getTitle());
        verify(eventRepository).findAll(pageable);
        verify(eventRepository, never()).findAllByStatus(any(), any());
    }

    @Test
    @DisplayName("Should return filtered events when status is provided")
    void findAllEvents_whenStatusIsProvided_shouldReturnFilteredEvents() {
        // Arrange
        List<Event> filteredList = Collections.singletonList(eventList.getFirst()); // Only the PENDING event
        Page<Event> eventPage = new PageImpl<>(filteredList, pageable, filteredList.size());
        when(eventRepository.findAllByStatus(EventStatus.PENDING, pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(EventStatus.PENDING, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Event", result.getContent().getFirst().getTitle());
        assertEquals(EventStatus.PENDING, result.getContent().getFirst().getStatus());
        verify(eventRepository).findAllByStatus(EventStatus.PENDING, pageable);
        verify(eventRepository, never()).findAll(same(pageable));
    }

    @Test
    @DisplayName("Should return event details by ID when user is admin")
    void findEventById_whenUserIsAdmin_shouldReturnEventDetails() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        EventDetailDTO result = eventQueryService.findEventById(eventId, userId, true);

        // Assert
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Test Event", result.getTitle());
        assertEquals(organizationId, result.getOrganizationId());
        assertEquals("Test Organization", result.getOrganizationName());
        verify(eventRepository).findById(eventId);
        verifyNoInteractions(eventOwnershipService); // Admin bypass, so no ownership check
    }

    @Test
    @DisplayName("Should return event details by ID when user is organization owner")
    void findEventById_whenUserIsOrganizationOwner_shouldReturnEventDetails() {
        // Arrange
        when(eventOwnershipService.verifyOwnershipAndGetEvent(eventId, userId)).thenReturn(event);

        // Act
        EventDetailDTO result = eventQueryService.findEventById(eventId, userId, false);

        // Assert
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Test Event", result.getTitle());
        assertEquals(organizationId, result.getOrganizationId());
        assertEquals("Test Organization", result.getOrganizationName());
        verify(eventOwnershipService).verifyOwnershipAndGetEvent(eventId, userId);
        verifyNoInteractions(eventRepository); // Using ownership service, not direct repository
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when event not found for admin")
    void findEventById_whenEventNotFoundForAdmin_shouldThrowResourceNotFoundException() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            eventQueryService.findEventById(eventId, userId, true));
        
        assertEquals("Event not found with ID: " + eventId, exception.getMessage());
        verify(eventRepository).findById(eventId);
        verifyNoInteractions(eventOwnershipService);
    }

    @Test
    @DisplayName("Should throw AuthorizationDeniedException when user is not organization owner")
    void findEventById_whenUserIsNotOrganizationOwner_shouldThrowAuthorizationDeniedException() {
        // Arrange
        when(eventOwnershipService.verifyOwnershipAndGetEvent(eventId, userId))
            .thenThrow(new AuthorizationDeniedException("User does not have access to this event"));

        // Act & Assert
        AuthorizationDeniedException exception = assertThrows(AuthorizationDeniedException.class, () -> 
            eventQueryService.findEventById(eventId, userId, false));
        
        assertEquals("You don't have permission to access this event", exception.getMessage());
        verify(eventOwnershipService).verifyOwnershipAndGetEvent(eventId, userId);
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("Should return event details by ID without authorization check")
    void findEventById_withoutAuthorizationCheck_shouldReturnEventDetails() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        EventDetailDTO result = eventQueryService.findEventById(eventId);

        // Assert
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Test Event", result.getTitle());
        verify(eventRepository).findById(eventId);
        verifyNoInteractions(eventOwnershipService); // No ownership check
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when event not found without authorization check")
    void findEventById_whenEventNotFoundWithoutAuthorizationCheck_shouldThrowResourceNotFoundException() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> 
            eventQueryService.findEventById(eventId));
        
        assertEquals("Event not found with ID: " + eventId, exception.getMessage());
        verify(eventRepository).findById(eventId);
        verifyNoInteractions(eventOwnershipService);
    }

    @Test
    @DisplayName("Should truncate long descriptions in event summary")
    void mapToEventSummary_withLongDescription_shouldTruncateDescription() {
        // Arrange
        String longDescription = "This is a very long description that exceeds 150 characters. " +
                "It should be truncated in the summary view to make the UI cleaner and more consistent. " +
                "The truncated text should end with three dots to indicate there's more content.";
        event.setDescription(longDescription);
        
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(event), pageable, 1);
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        String truncatedDescription = result.getContent().getFirst().getDescription();
        assertEquals(150, truncatedDescription.length());
        assertTrue(truncatedDescription.endsWith("..."));
    }

    @Test
    @DisplayName("Should handle events without sessions when finding earliest session date")
    void mapToEventSummary_withNoSessions_shouldReturnNullEarliestDate() {
        // Arrange
        event.setSessions(Collections.emptyList());
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(event), pageable, 1);
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().getFirst().getEarliestSessionDate());
    }

    @Test
    @DisplayName("Should handle events without cover photos")
    void mapToEventSummary_withNoCoverPhotos_shouldReturnNullCoverPhoto() {
        // Arrange
        event.setCoverPhotos(null);
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(event), pageable, 1);
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().getFirst().getCoverPhoto());
    }
}
