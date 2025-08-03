package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.config.TierLimitsConfig;
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
public class SubscriptionTierService {

    private final TierLimitsConfig tierLimitsConfig;
    private static final SubscriptionTier DEFAULT_TIER = SubscriptionTier.FREE;

    /**
     * Generic method to get any limit for a user based on their highest SubscriptionTier.
     * This class is now OPEN for extension (by adding to LimitType) but
     * CLOSED for modification.
     *
     * @param limitType The type of limit to retrieve (e.g., LimitType.MAX_ACTIVE_EVENTS).
     * @param jwt The user's JWT.
     * @return The integer value of the requested limit.
     */
    public int getLimit(SubscriptionLimitType limitType, Jwt jwt) {
        SubscriptionTier userTier = getHighestTierForUser(jwt);

        return Optional.ofNullable(tierLimitsConfig.getLimits().get(userTier.name()))
                .map(limitType.getLimitExtractor())
                .orElseGet(() -> limitType.getLimitExtractor()
                        .apply(tierLimitsConfig.getLimits().get(DEFAULT_TIER.name())));
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
