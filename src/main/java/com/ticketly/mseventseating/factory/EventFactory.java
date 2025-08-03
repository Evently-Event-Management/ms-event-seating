package com.ticketly.mseventseating.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.LayoutDataDTO;
import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.session.SessionRequest;
import com.ticketly.mseventseating.dto.tier.TierRequest;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.CategoryRepository;
import com.ticketly.mseventseating.repository.SeatingLayoutTemplateRepository;
import com.ticketly.mseventseating.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventFactory {

    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final SeatingLayoutTemplateRepository seatingLayoutTemplateRepository;
    private final ObjectMapper objectMapper;

    public Event createFromRequest(CreateEventRequest request, Organization organization) {
        Venue venue = findVenue(request.getVenueId());
        Set<Category> categories = findCategories(request.getCategoryIds());
        SeatingLayoutTemplate layoutTemplate = findLayoutTemplate(request.getSeatingLayoutTemplateId());

        Event event = buildEventEntity(request, organization, venue, categories);

        List<Tier> tiers = buildTiers(request.getTiers(), event);
        event.setTiers(tiers);

        List<EventSession> sessions = buildSessions(request.getSessions(), event, layoutTemplate);
        event.setSessions(sessions);

        return event;
    }

    private Event buildEventEntity(CreateEventRequest request, Organization org, Venue venue, Set<Category> categories) {
        return Event.builder()
                .title(request.getTitle()).description(request.getDescription()).overview(request.getOverview())
                .coverPhotos(request.getCoverPhotos()).organization(org).venue(venue).categories(categories)
                .isOnline(request.isOnline()).onlineLink(request.getOnlineLink())
                .locationDescription(request.getLocationDescription())
                // ✅ REMOVED: Sales rules are no longer set here.
                .build();
    }

    private List<Tier> buildTiers(List<TierRequest> tierRequests, Event event) {
        return tierRequests.stream()
                .map(req -> Tier.builder().name(req.getName()).price(req.getPrice()).color(req.getColor()).event(event).build())
                .collect(Collectors.toList());
    }

    private List<EventSession> buildSessions(List<SessionRequest> sessionRequests, Event event, SeatingLayoutTemplate layoutTemplate) {
        List<EventSession> sessions = new ArrayList<>();
        for (SessionRequest req : sessionRequests) {
            EventSession session = EventSession.builder()
                    .startTime(req.getStartTime())
                    .endTime(req.getEndTime())
                    .event(event)
                    .salesStartRuleType(req.getSalesStartRuleType())
                    .salesStartHoursBefore(req.getSalesStartHoursBefore())
                    .salesStartFixedDatetime(req.getSalesStartFixedDatetime())
                    .build();

            // Transform the layout template data into session seating map data
            String sessionLayoutData = transformLayoutDataForSession(layoutTemplate.getLayoutData());
            
            SessionSeatingMap map = SessionSeatingMap.builder()
                    .layoutData(sessionLayoutData)
                    .eventSession(session)
                    .build();
                    
            session.setSessionSeatingMap(map);
            sessions.add(session);
        }
        return sessions;
    }

    /**
     * Transforms the structural layout template data into an expanded session seating map
     * with generated IDs, seat rows and seat objects with initial statuses.
     *
     * @param templateLayoutData JSON string from the seating layout template
     * @return JSON string with the transformed layout data for the session
     */
    private String transformLayoutDataForSession(String templateLayoutData) {
        try {
            // Deserialize template layout data
            LayoutDataDTO templateLayout = objectMapper.readValue(templateLayoutData, LayoutDataDTO.class);
            
            // Create a new layout data object for the session
            LayoutDataDTO sessionLayout = new LayoutDataDTO();
            sessionLayout.setName(templateLayout.getName());
            
            LayoutDataDTO.Layout layout = new LayoutDataDTO.Layout();
            List<LayoutDataDTO.Block> transformedBlocks = new ArrayList<>();
            
            // Process each block in the template
            for (LayoutDataDTO.Block templateBlock : templateLayout.getLayout().getBlocks()) {
                LayoutDataDTO.Block sessionBlock = new LayoutDataDTO.Block();
                
                // Generate a new UUID for the block
                sessionBlock.setId(UUID.randomUUID().toString());
                sessionBlock.setName(templateBlock.getName());
                sessionBlock.setType(templateBlock.getType());
                sessionBlock.setPosition(templateBlock.getPosition());
                
                // Handle different block types
                if ("seated_grid".equals(templateBlock.getType())) {
                    // For seated grid, expand rows and columns into actual row and seat objects
                    sessionBlock.setRows(createRowsWithSeats(
                            templateBlock.getRowCount(),
                            templateBlock.getColumns(),
                            templateBlock.getStartRowLabel(),
                            templateBlock.getStartColumnLabel()
                    ));
                } else if ("standing_capacity".equals(templateBlock.getType())) {
                    // For standing capacity, initialize capacity, width, height, and sold count
                    sessionBlock.setCapacity(templateBlock.getCapacity());
                    sessionBlock.setWidth(templateBlock.getWidth());
                    sessionBlock.setHeight(templateBlock.getHeight());
                    sessionBlock.setTierId(null);
                    sessionBlock.setSoldCount(0);
                } else {
                    // For non-sellable blocks, just copy the properties
                    sessionBlock.setWidth(templateBlock.getWidth());
                    sessionBlock.setHeight(templateBlock.getHeight());
                }
                
                transformedBlocks.add(sessionBlock);
            }
            
            layout.setBlocks(transformedBlocks);
            sessionLayout.setLayout(layout);
            
            // Serialize back to JSON
            return objectMapper.writeValueAsString(sessionLayout);
            
        } catch (IOException e) {
            log.error("Failed to transform layout data", e);
            throw new RuntimeException("Failed to transform layout data", e);
        }
    }
    
    /**
     * Creates rows with seats based on the specified number of rows and columns
     * with optional custom starting row label and column number
     */
    private List<LayoutDataDTO.Row> createRowsWithSeats(int numRows, int numColumns, 
                                                       String startRowLabel, Integer startColumnLabel) {
        List<LayoutDataDTO.Row> rows = new ArrayList<>();
        
        // Determine starting column number, default to 1 if not specified
        int startColumn = 1;
        if (startColumnLabel != null) {
            startColumn = startColumnLabel;
        }
        
        // Generate row labels starting from the specified label
        List<String> rowLabels = generateRowLabels(numRows, startRowLabel);
        
        for (int i = 0; i < numRows; i++) {
            LayoutDataDTO.Row row = new LayoutDataDTO.Row();
            row.setId(UUID.randomUUID().toString());
            row.setLabel(rowLabels.get(i));
            
            // Create seats for this row
            List<LayoutDataDTO.Seat> seats = new ArrayList<>();
            for (int j = 0; j < numColumns; j++) {
                LayoutDataDTO.Seat seat = new LayoutDataDTO.Seat();
                seat.setId(UUID.randomUUID().toString());
                seat.setLabel(String.valueOf(startColumn + j));
                seat.setTierId(null);
                seat.setStatus("AVAILABLE");
                seats.add(seat);
            }
            
            row.setSeats(seats);
            rows.add(row);
        }
        
        return rows;
    }
    
    /**
     * Generates alphabetical row labels starting from a specified label
     * Supports multi-character labels (e.g., "AB" → "AC" → "AD" → ...)
     * 
     * @param count number of row labels to generate
     * @param startLabel the starting label (default is "A" if null or empty)
     * @return List of generated row labels
     */
    private List<String> generateRowLabels(int count, String startLabel) {
        List<String> labels = new ArrayList<>(count);
        
        // Default to "A" if startLabel is null or empty
        if (startLabel == null || startLabel.isEmpty()) {
            startLabel = "A";
        }
        
        // Start with the initial label
        labels.add(startLabel);
        
        // Generate subsequent labels
        for (int i = 1; i < count; i++) {
            String prevLabel = labels.get(i - 1);
            labels.add(incrementLabel(prevLabel));
        }
        
        return labels;
    }
    
    /**
     * Increments an alphabetical label (e.g., "A" → "B", "Z" → "AA", "AB" → "AC")
     * 
     * @param label the label to increment
     * @return the next label in sequence
     */
    private String incrementLabel(String label) {
        // Convert to char array for easier manipulation
        char[] chars = label.toCharArray();
        
        // Start from the rightmost character and try to increment
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] < 'Z') {
                // If not 'Z', simply increment and we're done
                chars[i]++;
                return new String(chars);
            } else {
                // This position is 'Z', roll over to 'A'
                chars[i] = 'A';
                // Continue loop to increment the next position to the left
            }
        }
        
        // If we get here, all characters were 'Z'
        // So we need to add one more 'A' at the beginning
        return "A" + new String(chars);
    }

    private Venue findVenue(UUID venueId) {
        if (venueId == null) return null;
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + venueId));
    }

    private Set<Category> findCategories(Set<UUID> categoryIds) {
        return new java.util.HashSet<>(categoryRepository.findAllById(categoryIds));
    }

    private SeatingLayoutTemplate findLayoutTemplate(UUID templateId) {
        return seatingLayoutTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Seating layout template not found with ID: " + templateId));
    }
}