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

    /**
     * Retrieves the complete, consolidated application configuration for the frontend.
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
     *
     * @param jwt The user's JWT token.
     * @return A DTO containing the user's specific tier limits and general app limits.
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

    /**
     * Generic method for internal services to get any limit for a user based on their highest tier.
     */
    public int getLimit(SubscriptionLimitType limitType, Jwt jwt) {
        SubscriptionTier userTier = getHighestTierForUser(jwt);

        return Optional.ofNullable(appLimitsConfig.getTier().getLimits().get(userTier.name()))
                .map(limitType.getLimitExtractor())
                .orElseGet(() -> limitType.getLimitExtractor()
                        .apply(appLimitsConfig.getTier().getLimits().get(DEFAULT_TIER.name())));
    }

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
