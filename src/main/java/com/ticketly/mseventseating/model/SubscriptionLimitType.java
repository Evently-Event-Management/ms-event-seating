package com.ticketly.mseventseating.model;

import com.ticketly.mseventseating.config.TierLimitsConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum SubscriptionLimitType {
    MAX_ORGANIZATIONS_PER_USER(TierLimitsConfig.TierLimitDetails::getMaxOrganizationsPerUser),
    MAX_SEATING_LAYOUTS_PER_ORG(TierLimitsConfig.TierLimitDetails::getMaxSeatingLayoutsPerOrg),
    MAX_ACTIVE_EVENTS(TierLimitsConfig.TierLimitDetails::getMaxActiveEvents),
    MAX_SESSIONS_PER_EVENT(TierLimitsConfig.TierLimitDetails::getMaxSessionsPerEvent);

    // This holds the "strategy" for extracting the limit value.
    private final Function<TierLimitsConfig.TierLimitDetails, Integer> limitExtractor;
}
