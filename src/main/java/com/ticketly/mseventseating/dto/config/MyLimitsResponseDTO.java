package com.ticketly.mseventseating.dto.config;

import com.ticketly.mseventseating.config.AppLimitsConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MyLimitsResponseDTO {
    private String currentTier;
    private AppLimitsConfig.TierLimitDetails tierLimits;
    private AppLimitsConfig.OrganizationConfig organizationLimits;
    private AppLimitsConfig.EventConfig eventLimits;
    private AppLimitsConfig.SeatingLayoutConfig seatingLayoutConfig;
}
