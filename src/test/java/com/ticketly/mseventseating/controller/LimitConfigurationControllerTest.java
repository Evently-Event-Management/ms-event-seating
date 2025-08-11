package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.config.AppLimitsConfig;
import com.ticketly.mseventseating.dto.config.AppConfigDTO;
import com.ticketly.mseventseating.dto.config.MyLimitsResponseDTO;
import com.ticketly.mseventseating.service.LimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LimitConfigurationControllerTest {

    @Mock
    private LimitService limitService;

    @InjectMocks
    private LimitConfigurationController limitConfigurationController;

    private Jwt jwt;
    private AppConfigDTO appConfigDTO;
    private MyLimitsResponseDTO myLimitsResponseDTO;

    @BeforeEach
    void setUp() {
        // Setup JWT
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        HashMap<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-123");

        jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), headers, claims);

        // Setup mock tier limit details
        AppLimitsConfig.TierLimitDetails tierLimitDetails = new AppLimitsConfig.TierLimitDetails();
        tierLimitDetails.setMaxOrganizationsPerUser(3);
        tierLimitDetails.setMaxActiveEvents(10);
        tierLimitDetails.setMaxSeatingLayoutsPerOrg(5);

        // Setup mock organization config
        AppLimitsConfig.OrganizationConfig organizationConfig = new AppLimitsConfig.OrganizationConfig();
        organizationConfig.setMaxLogoSize(512);

        // Setup mock event config
        AppLimitsConfig.EventConfig eventConfig = new AppLimitsConfig.EventConfig();
        eventConfig.setMaxCoverPhotoSize(1024);
        eventConfig.setMaxCoverPhotos(5);

        // Setup mock seating layout config
        AppLimitsConfig.SeatingLayoutConfig seatingLayoutConfig = new AppLimitsConfig.SeatingLayoutConfig();
        seatingLayoutConfig.setDefaultPageSize(6);
        seatingLayoutConfig.setDefaultGap(3);

        // Setup AppConfigDTO
        Map<String, AppLimitsConfig.TierLimitDetails> tierLimits = new HashMap<>();
        tierLimits.put("FREE", tierLimitDetails);
        tierLimits.put("STANDARD", tierLimitDetails);
        tierLimits.put("PREMIUM", tierLimitDetails);

        appConfigDTO = AppConfigDTO.builder()
                .tierLimits(tierLimits)
                .organizationLimits(organizationConfig)
                .eventLimits(eventConfig)
                .seatingLayoutConfig(seatingLayoutConfig)
                .build();

        // Setup MyLimitsResponseDTO
        myLimitsResponseDTO = MyLimitsResponseDTO.builder()
                .currentTier("STANDARD")
                .tierLimits(tierLimitDetails)
                .organizationLimits(organizationConfig)
                .eventLimits(eventConfig)
                .seatingLayoutConfig(seatingLayoutConfig)
                .build();
    }

    @Test
    void getAppConfiguration_ShouldReturnAppConfig() {
        // Arrange
        when(limitService.getAppConfiguration()).thenReturn(appConfigDTO);

        // Act
        ResponseEntity<AppConfigDTO> response = limitConfigurationController.getAppConfiguration();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(appConfigDTO, response.getBody());

        // Verify service method was called
        verify(limitService).getAppConfiguration();
    }

    @Test
    void getMyLimits_ShouldReturnUserSpecificLimits() {
        // Arrange
        when(limitService.getMyLimits(any(Jwt.class))).thenReturn(myLimitsResponseDTO);

        // Act
        ResponseEntity<MyLimitsResponseDTO> response = limitConfigurationController.getMyLimits(jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(myLimitsResponseDTO, response.getBody());
        assertEquals("STANDARD", response.getBody().getCurrentTier());

        // Verify service method was called with JWT
        verify(limitService).getMyLimits(jwt);
    }
}
