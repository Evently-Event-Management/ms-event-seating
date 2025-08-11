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
    //For physical events
    private String address;
    private String name;
    private Double latitude;
    private Double longitude;

    //For online events
    private String onlineLink;
}