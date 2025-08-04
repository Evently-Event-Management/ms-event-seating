package com.ticketly.mseventseating.model;

import com.ticketly.mseventseating.config.AppLimitsConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum SubscriptionLimitType {
    MAX_ORGANIZATIONS_PER_USER(AppLimitsConfig.TierLimitDetails::getMaxOrganizationsPerUser),
    MAX_SEATING_LAYOUTS_PER_ORG(AppLimitsConfig.TierLimitDetails::getMaxSeatingLayoutsPerOrg),
    MAX_SESSIONS_PER_EVENT(AppLimitsConfig.TierLimitDetails::getMaxSessionsPerEvent);

    // This holds the "strategy" for extracting the limit value.
    private final Function<AppLimitsConfig.TierLimitDetails, Integer> limitExtractor;
}
