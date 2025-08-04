package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.config.AppLimitsConfig;
import com.ticketly.mseventseating.dto.config.AppConfigDTO;
import com.ticketly.mseventseating.dto.config.MyLimitsResponseDTO;
import com.ticketly.mseventseating.model.SubscriptionLimitType;
import com.ticketly.mseventseating.model.SubscriptionTier;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LimitService {

    private final AppLimitsConfig appLimitsConfig;
    private static final SubscriptionTier DEFAULT_TIER = SubscriptionTier.FREE;

    // ================================================================================
    // Methods for Controllers (High-Level DTOs for the Frontend)
    // ================================================================================

    /**
     * Retrieves the complete, consolidated application configuration.
     * Intended for use by the LimitConfigurationController for pages like "Pricing".
     */
    public AppConfigDTO getAppConfiguration() {
        return AppConfigDTO.builder()
                .tierLimits(appLimitsConfig.getTier().getLimits())
                .organizationLimits(appLimitsConfig.getOrganization())
                .eventLimits(appLimitsConfig.getEvent())
                .seatingLayoutConfig(appLimitsConfig.getSeatingLayout())
                .build();
    }

    /**
     * Retrieves the limits and configuration relevant to the currently authenticated user.
     * Intended for use by the LimitConfigurationController.
     */
    public MyLimitsResponseDTO getMyLimits(Jwt jwt) {
        SubscriptionTier userTier = getHighestTierForUser(jwt);
        AppLimitsConfig.TierLimitDetails userTierLimits = appLimitsConfig.getTier().getLimits().get(userTier.name());

        return MyLimitsResponseDTO.builder()
                .currentTier(userTier.name())
                .tierLimits(userTierLimits)
                .organizationLimits(appLimitsConfig.getOrganization())
                .eventLimits(appLimitsConfig.getEvent())
                .seatingLayoutConfig(appLimitsConfig.getSeatingLayout())
                .build();
    }

    // ================================================================================
    // Methods for Internal Services (Specific, Granular Configs & Limits)
    // ================================================================================

    /**
     * Generic method for internal services to get any TIER-BASED limit for a user.
     * This replaces the need for other services to use @Value for tier-specific limits.
     */
    public int getTierLimit(SubscriptionLimitType limitType, Jwt jwt) {
        SubscriptionTier userTier = getHighestTierForUser(jwt);

        return Optional.ofNullable(appLimitsConfig.getTier().getLimits().get(userTier.name()))
                .map(limitType.getLimitExtractor())
                .orElseGet(() -> limitType.getLimitExtractor()
                        .apply(appLimitsConfig.getTier().getLimits().get(DEFAULT_TIER.name())));
    }

    /**
     * Provides the general, non-tier-specific configuration for events.
     */
    public AppLimitsConfig.EventConfig getEventConfig() {
        return appLimitsConfig.getEvent();
    }

    /**
     * Provides the general, non-tier-specific configuration for organizations.
     */
    public AppLimitsConfig.OrganizationConfig getOrganizationConfig() {
        return appLimitsConfig.getOrganization();
    }

    /**
     * Provides the general, non-tier-specific configuration for seating layouts.
     */
    public AppLimitsConfig.SeatingLayoutConfig getSeatingLayoutConfig() {
        return appLimitsConfig.getSeatingLayout();
    }


    // ================================================================================
    // Private Helper Methods
    // ================================================================================

    private SubscriptionTier getHighestTierForUser(Jwt jwt) {
        List<String> userGroups = jwt.getClaimAsStringList("user_groups");

        if (userGroups == null || userGroups.isEmpty()) {
            return DEFAULT_TIER;
        }

        return userGroups.stream()
                .map(this::parseTierFromGroup)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(SubscriptionTier::getLevel))
                .orElse(DEFAULT_TIER);
    }

    private Optional<SubscriptionTier> parseTierFromGroup(String group) {
        if (group == null || !group.startsWith("/Tiers/")) {
            return Optional.empty();
        }
        try {
            String tierName = group.substring("/Tiers/".length()).toUpperCase();
            return Optional.of(SubscriptionTier.valueOf(tierName));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
