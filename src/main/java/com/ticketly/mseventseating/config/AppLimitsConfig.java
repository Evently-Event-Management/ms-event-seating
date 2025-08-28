package com.ticketly.mseventseating.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppLimitsConfig {
    private Map<String, TierLimitDetails> limits;
    private TierConfig tier;
    private OrganizationConfig organization;
    private EventConfig event;
    private SeatingLayoutConfig seatingLayout;

    @Data
    public static class TierConfig {
        private Map<String, TierLimitDetails> limits;
    }

    @Data
    public static class TierLimitDetails {
        private int maxOrganizationsPerUser;
        private int maxSeatingLayoutsPerOrg;
        private int maxActiveEvents;
        private int maxSessionsPerEvent;
    }

    @Data
    public static class OrganizationConfig {
        private long maxLogoSize;
    }

    @Data
    public static class EventConfig {
        private int maxCoverPhotos;
        private long maxCoverPhotoSize;
    }
    
    @Data
    public static class SeatingLayoutConfig {
        private int defaultPageSize;
        private int defaultGap;
    }
}
