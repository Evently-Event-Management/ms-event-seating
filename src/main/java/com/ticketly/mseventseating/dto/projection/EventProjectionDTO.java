package com.ticketly.mseventseating.dto.projection;

import com.ticketly.mseventseating.model.EventStatus;
import com.ticketly.mseventseating.model.SessionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EventProjectionDTO {
    private UUID id;
    private String title;
    private String description;
    private String overview;
    private EventStatus status;
    private List<String> coverPhotos;
    private OrganizationInfo organization;
    private CategoryInfo category;
    private List<TierInfo> tiers; // Keep this for a summary view of all available tiers
    private List<SessionInfo> sessions;

    @Data
    @Builder
    public static class OrganizationInfo {
        private UUID id;
        private String name;
        private String logoUrl;
    }

    @Data
    @Builder
    public static class CategoryInfo {
        private UUID id;
        private String name;
        private String parentName;
    }

    @Data
    @Builder
    public static class TierInfo {
        private UUID id;
        private String name;
        private BigDecimal price;
        private String color;
    }

    @Data
    @Builder
    public static class SessionInfo {
        private UUID id;
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private String status;
        private SessionType sessionType;
        private VenueDetailsInfo venueDetails;
        // ✅ Updated to use the new, fully denormalized seating map structure
        private SessionSeatingMapInfo layoutData;
    }

    @Data
    @Builder
    public static class VenueDetailsInfo {
        private String name;
        private String address;
        private String onlineLink;
        private GeoJsonPoint location;
    }

    @Data
    @Builder
    public static class GeoJsonPoint {
        private final String type = "Point";
        private double[] coordinates; // [longitude, latitude]
    }

    // ✅ NEW: Redefined DTOs for the seating map are now nested here for clarity.
    @Data
    @Builder
    public static class SessionSeatingMapInfo {
        private String name;
        private LayoutInfo layout;
    }

    @Data
    @Builder
    public static class LayoutInfo {
        private List<BlockInfo> blocks;
    }

    @Data
    @Builder
    public static class BlockInfo {
        private String id;
        private String name;
        private String type;
        private PositionInfo position;
        private List<RowInfo> rows;
        private List<SeatInfo> seats; // For standing capacity blocks
        private Integer capacity;
        private Integer width;
        private Integer height;
    }

    @Data
    @Builder
    public static class RowInfo {
        private String id;
        private String label;
        private List<SeatInfo> seats;
    }

    @Data
    @Builder
    public static class SeatInfo {
        private String id;
        private String label;
        private String status;
        private TierInfo tier;
    }

    @Data
    @Builder
    public static class PositionInfo {
        private Double x;
        private Double y;
    }
}
