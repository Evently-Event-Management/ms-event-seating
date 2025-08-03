package com.ticketly.mseventseating.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.tier")
@Data
public class TierLimitsConfig {
    private Map<String, TierLimitDetails> limits;

    @Data
    public static class TierLimitDetails {
        private int maxOrganizationsPerUser;
        private int maxSeatingLayoutsPerOrg;
        private int maxActiveEvents;
        private int maxSessionsPerEvent;
    }
}
