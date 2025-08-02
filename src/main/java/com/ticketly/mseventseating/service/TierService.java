package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.config.TierLimitsConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
enum Tier {
    FREE(0),
    PRO(1),
    ENTERPRISE(2);

    private final int level;
}

@Service
@RequiredArgsConstructor
public class TierService {

    private final TierLimitsConfig tierLimitsConfig;
    private static final Tier DEFAULT_TIER = Tier.FREE;

    public int getMaxOrganizationsForUser(Jwt jwt) {
        Tier userTier = getHighestTierForUser(jwt);
        // ✅ Safely get the limit, falling back to the default if the tier is not configured
        return Optional.ofNullable(tierLimitsConfig.getLimits().get(userTier.name()))
                .map(TierLimitsConfig.TierLimitDetails::getMaxOrganizationsPerUser)
                .orElseGet(() -> tierLimitsConfig.getLimits().get(DEFAULT_TIER.name()).getMaxOrganizationsPerUser());
    }

    public int getMaxSeatingLayoutsForOrg(Jwt jwt) {
        Tier userTier = getHighestTierForUser(jwt);
        // ✅ Safely get the limit, falling back to the default
        return Optional.ofNullable(tierLimitsConfig.getLimits().get(userTier.name()))
                .map(TierLimitsConfig.TierLimitDetails::getMaxSeatingLayoutsPerOrg)
                .orElseGet(() -> tierLimitsConfig.getLimits().get(DEFAULT_TIER.name()).getMaxSeatingLayoutsPerOrg());
    }

    private Tier getHighestTierForUser(Jwt jwt) {
        List<String> userGroups = jwt.getClaimAsStringList("user_groups");

        if (userGroups == null || userGroups.isEmpty()) {
            return DEFAULT_TIER;
        }

        return userGroups.stream()
                .map(this::parseTierFromGroup)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(Tier::getLevel))
                .orElse(DEFAULT_TIER);
    }

    private Optional<Tier> parseTierFromGroup(String group) {
        if (group == null || !group.startsWith("/Tiers/")) {
            return Optional.empty();
        }
        try {
            String tierName = group.substring("/Tiers/".length()).toUpperCase();
            return Optional.of(Tier.valueOf(tierName));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
