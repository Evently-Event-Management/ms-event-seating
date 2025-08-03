package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.config.TierLimitsConfig;
import com.ticketly.mseventseating.model.SubscriptionLimitType;
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
class SubscriptionTierServiceTest {

    @Mock
    private TierLimitsConfig tierLimitsConfig;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private SubscriptionTierService subscriptionTierService;

    @BeforeEach
    void setUp() {
        // Setup tier limits
        Map<String, TierLimitsConfig.TierLimitDetails> tierLimits = new HashMap<>();

        TierLimitsConfig.TierLimitDetails freeTier = new TierLimitsConfig.TierLimitDetails();
        freeTier.setMaxOrganizationsPerUser(1);
        freeTier.setMaxSeatingLayoutsPerOrg(3);
        freeTier.setMaxActiveEvents(2);
        freeTier.setMaxSessionsPerEvent(3);
        tierLimits.put("FREE", freeTier);

        TierLimitsConfig.TierLimitDetails proTier = new TierLimitsConfig.TierLimitDetails();
        proTier.setMaxOrganizationsPerUser(3);
        proTier.setMaxSeatingLayoutsPerOrg(10);
        proTier.setMaxActiveEvents(10);
        proTier.setMaxSessionsPerEvent(15);
        tierLimits.put("PRO", proTier);

        TierLimitsConfig.TierLimitDetails enterpriseTier = new TierLimitsConfig.TierLimitDetails();
        enterpriseTier.setMaxOrganizationsPerUser(10);
        enterpriseTier.setMaxSeatingLayoutsPerOrg(50);
        enterpriseTier.setMaxActiveEvents(100);
        enterpriseTier.setMaxSessionsPerEvent(100);
        tierLimits.put("ENTERPRISE", enterpriseTier);

        when(tierLimitsConfig.getLimits()).thenReturn(tierLimits);
    }

    @Test
    void getMaxOrganizationsForUser_WithFreeTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/FREE"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);

        // Assert
        assertEquals(1, result);
    }

    @Test
    void getMaxOrganizationsForUser_WithProTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/PRO"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);

        // Assert
        assertEquals(3, result);
    }

    @Test
    void getMaxOrganizationsForUser_WithEnterpriseTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/ENTERPRISE"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);

        // Assert
        assertEquals(10, result);
    }

    @Test
    void getMaxSeatingLayoutsForOrg_WithFreeTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/FREE"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG, jwt);

        // Assert
        assertEquals(3, result);
    }

    @Test
    void getMaxSeatingLayoutsForOrg_WithProTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/PRO"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG, jwt);

        // Assert
        assertEquals(10, result);
    }

    @Test
    void getMaxOrganizationsForUser_WithMultipleTiers_ShouldUseHighest() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Arrays.asList("/Tiers/FREE", "/Tiers/PRO"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);

        // Assert
        assertEquals(3, result); // Should use PRO tier (higher level)
    }

    @Test
    void getMaxOrganizationsForUser_WithNoTier_ShouldUseFreeTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.emptyList());

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);

        // Assert
        assertEquals(1, result); // Should use FREE tier (default)
    }

    @Test
    void getMaxOrganizationsForUser_WithNullGroups_ShouldUseFreeTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(null);

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);

        // Assert
        assertEquals(1, result); // Should use FREE tier (default)
    }

    @Test
    void getMaxOrganizationsForUser_WithInvalidTier_ShouldIgnoreThem() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Arrays.asList("/Tiers/INVALID", "/Other/Group"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);

        // Assert
        assertEquals(1, result); // Should use FREE tier (default)
    }

    @Test
    void getMaxOrganizationsForUser_WithMixedValidAndInvalidTiers_ShouldUseValidOnes() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(
                Arrays.asList("/Tiers/INVALID", "/Tiers/PRO", "/Other/Group")
        );

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt);

        // Assert
        assertEquals(3, result); // Should use PRO tier
    }

    @Test
    void getMaxActiveEvents_WithFreeTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/FREE"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);

        // Assert
        assertEquals(2, result);
    }

    @Test
    void getMaxActiveEvents_WithProTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/PRO"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);

        // Assert
        assertEquals(10, result);
    }

    @Test
    void getMaxActiveEvents_WithEnterpriseTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/ENTERPRISE"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);

        // Assert
        assertEquals(100, result);
    }

    @Test
    void getMaxActiveEvents_WithMultipleTiers_ShouldUseHighest() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Arrays.asList("/Tiers/FREE", "/Tiers/PRO"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);

        // Assert
        assertEquals(10, result); // Should use PRO tier (higher level)
    }

    @Test
    void getMaxSessionsPerEvent_WithFreeTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/FREE"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);

        // Assert
        assertEquals(3, result);
    }

    @Test
    void getMaxSessionsPerEvent_WithProTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/PRO"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);

        // Assert
        assertEquals(15, result);
    }

    @Test
    void getMaxSessionsPerEvent_WithEnterpriseTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.singletonList("/Tiers/ENTERPRISE"));

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);

        // Assert
        assertEquals(100, result);
    }

    @Test
    void getMaxSessionsPerEvent_WithNoTier_ShouldUseFreeTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(Collections.emptyList());

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);

        // Assert
        assertEquals(3, result); // Should use FREE tier (default)
    }

    @Test
    void getMaxSessionsPerEvent_WithNullGroups_ShouldUseFreeTier() {
        // Arrange
        when(jwt.getClaimAsStringList("user_groups")).thenReturn(null);

        // Act
        int result = subscriptionTierService.getLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);

        // Assert
        assertEquals(3, result); // Should use FREE tier (default)
    }
}
