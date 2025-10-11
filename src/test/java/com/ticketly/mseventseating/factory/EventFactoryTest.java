package com.ticketly.mseventseating.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.SessionRequest;
import com.ticketly.mseventseating.dto.event.TierRequest;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.CategoryRepository;
import dto.SessionSeatingMapDTO;
import model.SessionStatus;
import model.SessionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventFactoryTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventFactory eventFactory;

    private CreateEventRequest eventRequest;
    private Organization organization;
    private Category category;
    private UUID categoryId;
    private UUID sessionId;
    private UUID tierId;
    private SessionRequest sessionRequest;
    private TierRequest tierRequest;

    @BeforeEach
    void setUp() throws Exception {
        categoryId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        tierId = UUID.randomUUID();
        
        // Setup category
        category = Category.builder()
                .id(categoryId)
                .name("Concert")
                .build();

        // Setup organization
        organization = Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Organization")
                .build();

        // Setup tier request
        tierRequest = new TierRequest();
        tierRequest.setId(tierId);
        tierRequest.setName("VIP");
        tierRequest.setPrice(new java.math.BigDecimal("100.00"));
        tierRequest.setColor("#FF0000");

        // Setup session request with venue details
        sessionRequest = new SessionRequest();
        sessionRequest.setId(sessionId);
        sessionRequest.setStartTime(OffsetDateTime.now().plusDays(7));
        sessionRequest.setEndTime(OffsetDateTime.now().plusDays(7).plusHours(2));
        sessionRequest.setSalesStartTime(OffsetDateTime.now().plusDays(1));
        sessionRequest.setSessionType(SessionType.PHYSICAL);
        
        // Mock layout data
        SessionSeatingMapDTO layoutData = new SessionSeatingMapDTO();
        SessionSeatingMapDTO.Layout layout = new SessionSeatingMapDTO.Layout();
        layout.setBlocks(Collections.emptyList());
        layoutData.setLayout(layout);
        
        sessionRequest.setLayoutData(layoutData);

        // Setup event request
        eventRequest = new CreateEventRequest();
        eventRequest.setCategoryId(categoryId);
        eventRequest.setTitle("Test Event");
        eventRequest.setDescription("This is a test event");
        eventRequest.setOverview("Test event overview");
        eventRequest.setTiers(List.of(tierRequest));
        eventRequest.setSessions(List.of(sessionRequest));
        eventRequest.setDiscounts(Collections.emptyList());

        // Setup repository mock
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        
        // Setup ObjectMapper mock for JSON serialization
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        // Since we're not testing discounts in this test, we'll just make sure
        // the eventRequest doesn't have any discounts
        eventRequest.setDiscounts(Collections.emptyList());
    }

    @Test
    void createFromRequest_ShouldCreateEventWithSessionsInScheduledStatus() {
        // Act
        Event event = eventFactory.createFromRequest(eventRequest, organization, Collections.emptyList());

        // Assert
        assertNotNull(event);
        assertEquals("Test Event", event.getTitle());
        assertEquals("This is a test event", event.getDescription());
        assertEquals(organization, event.getOrganization());
        assertEquals(category, event.getCategory());
        
        // Verify session creation with SCHEDULED status
        assertNotNull(event.getSessions());
        assertEquals(1, event.getSessions().size());
        
        EventSession createdSession = event.getSessions().get(0);
        assertEquals(SessionStatus.SCHEDULED, createdSession.getStatus());
        assertEquals(sessionRequest.getStartTime(), createdSession.getStartTime());
        assertEquals(sessionRequest.getEndTime(), createdSession.getEndTime());
        assertEquals(sessionRequest.getSalesStartTime(), createdSession.getSalesStartTime());
        assertEquals(sessionRequest.getSessionType(), createdSession.getSessionType());
        
        // Verify tier creation
        assertNotNull(event.getTiers());
        assertEquals(1, event.getTiers().size());
        
        Tier createdTier = event.getTiers().get(0);
        assertEquals(tierRequest.getName(), createdTier.getName());
        assertEquals(tierRequest.getPrice(), createdTier.getPrice());
        assertEquals(tierRequest.getColor(), createdTier.getColor());
    }
}