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
    private List<TierInfo> tiers;
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
        private double[] coordinates = new double[2]; // [longitude, latitude]
    }
}
