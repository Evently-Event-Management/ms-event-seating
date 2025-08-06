package com.ticketly.mseventseating.dto.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VenueDetailsDTO {
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
}