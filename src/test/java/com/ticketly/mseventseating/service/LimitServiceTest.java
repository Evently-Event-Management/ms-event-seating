package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.config.AppLimitsConfig;
import com.ticketly.mseventseating.model.SubscriptionLimitType;
import com.ticketly.mseventseating.model.SubscriptionTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitServiceTest { // ✅ Renamed for clarity

    @Mock
    private AppLimitsConfig appLimitsConfig;

    @Mock
    private AppLimitsConfig.TierConfig tierConfig; // ✅ Mock the nested TierConfig

    @Mock
    private Jwt jwt;

    @InjectMocks
    private LimitService limitService;

    @BeforeEach
    void setUp() {
        // --- Setup tier limits map ---
        Map<String, AppLimitsConfig.TierLimitDetails> tierLimits = new HashMap<>();

        AppLimitsConfig.TierLimitDetails freeTier = new AppLimitsConfig.TierLimitDetails();
        freeTier.setMaxOrganizationsPerUser(1);
        freeTier.setMaxSeatingLayoutsPerOrg(3);
        freeTier.setMaxActiveEvents(2);
        freeTier.setMaxSessionsPerEvent(3);
        tierLimits.put("FREE", freeTier);

        AppLimitsConfig.TierLimitDetails proTier = new AppLimitsConfig.TierLimitDetails();
        proTier.setMaxOrganizationsPerUser(3);
        proTier.setMaxSeatingLayoutsPerOrg(10);
        proTier.setMaxActiveEvents(10);
        proTier.setMaxSessionsPerEvent(15);
        tierLimits.put("PRO", proTier);

        AppLimitsConfig.TierLimitDetails enterpriseTier = new AppLimitsConfig.TierLimitDetails();
        enterpriseTier.setMaxOrganizationsPerUser(10);
        enterpriseTier.setMaxSeatingLayoutsPerOrg(50);
        enterpriseTier.setMaxActiveEvents(100);
        enterpriseTier.setMaxSessionsPerEvent(100);
        tierLimits.put("ENTERPRISE", enterpriseTier);

        // ✅ Corrected Mocking: Mock the chain of calls
        when(appLimitsConfig.getTier()).thenReturn(tierConfig);
        when(tierConfig.getLimits()).thenReturn(tierLimits);
    }

    @Test
    void getLimit_ForMaxOrganizations_WithFreeTier() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/FREE"));
        int result = limitService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);
        assertEquals(1, result);
    }

    @Test
    void getLimit_ForMaxOrganizations_WithProTier() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/PRO"));
        int result = limitService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);
        assertEquals(3, result);
    }

    @Test
    void getLimit_ForMaxOrganizations_WithEnterpriseTier() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/ENTERPRISE"));
        int result = limitService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);
        assertEquals(10, result);
    }

    @Test
    void getLimit_ForMaxSeatingLayouts_WithProTier() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/PRO"));
        int result = limitService.getLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG, jwt);
        assertEquals(10, result);
    }

    @Test
    void getLimit_WithMultipleTiers_ShouldUseHighest() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Arrays.asList("/Tiers/FREE", "/Tiers/PRO"));
        int result = limitService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);
        assertEquals(3, result); // Should use PRO tier (higher level)
    }

    @Test
    void getLimit_WithNoTier_ShouldUseFreeTier() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.emptyList());
        int result = limitService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);
        assertEquals(1, result); // Should use FREE tier (default)
    }

    @Test
    void getLimit_WithNullGroups_ShouldUseFreeTier() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(null);
        int result = limitService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);
        assertEquals(1, result); // Should use FREE tier (default)
    }

    @Test
    void getLimit_WithInvalidTier_ShouldUseFreeTier() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Arrays.asList("/Tiers/INVALID", "/Other/Group"));
        int result = limitService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);
        assertEquals(1, result); // Should use FREE tier (default)
    }

    @Test
    void getLimit_WithMixedValidAndInvalidTiers_ShouldUseValidHighest() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(
                Arrays.asList("/Tiers/INVALID", "/Tiers/PRO", "/Tiers/FREE")
        );
        int result = limitService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);
        assertEquals(3, result); // Should use PRO tier
    }

    @Test
    void getLimit_ForMaxActiveEvents_WithProTier() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/PRO"));
        int result = limitService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        assertEquals(10, result);
    }

    @Test
    void getLimit_ForMaxSessionsPerEvent_WithEnterpriseTier() {
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/ENTERPRISE"));
        int result = limitService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        assertEquals(100, result);
    }
}
