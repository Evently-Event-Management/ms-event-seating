package com.ticketly.mseventseating.dto.config;

import com.ticketly.mseventseating.config.AppLimitsConfig;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AppConfigDTO {
    private Map<String, AppLimitsConfig.TierLimitDetails> tierLimits;
    private AppLimitsConfig.OrganizationConfig organizationLimits;
    private AppLimitsConfig.EventConfig eventLimits;
    private AppLimitsConfig.SeatingLayoutConfig seatingLayoutConfig;
}
