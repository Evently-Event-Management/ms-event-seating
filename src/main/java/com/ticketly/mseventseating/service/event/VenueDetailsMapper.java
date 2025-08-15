package com.ticketly.mseventseating.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.VenueDetailsDTO;
import com.ticketly.mseventseating.dto.projection.SessionProjectionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VenueDetailsMapper {
    private final ObjectMapper objectMapper;

    public VenueDetailsDTO parseVenueDetails(String json) {
        try {
            if (json == null) return null;
            return objectMapper.readValue(json, VenueDetailsDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    public SessionProjectionDTO.VenueDetailsInfo mapToVenueDetailsInfo(VenueDetailsDTO dto) {
        if (dto == null) return null;
        SessionProjectionDTO.GeoJsonPoint point = null;
        if (dto.getLongitude() != null && dto.getLatitude() != null) {
            point = SessionProjectionDTO.GeoJsonPoint.builder()
                    .coordinates(new double[]{dto.getLongitude(), dto.getLatitude()})
                    .build();
        }
        return SessionProjectionDTO.VenueDetailsInfo.builder()
                .name(dto.getName()).address(dto.getAddress()).onlineLink(dto.getOnlineLink()).location(point)
                .build();
    }
}

