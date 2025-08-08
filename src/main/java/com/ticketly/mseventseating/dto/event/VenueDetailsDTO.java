package com.ticketly.mseventseating.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VenueDetailsDTO {
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
}