package com.ticketly.mseventseating.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueRequest {

    @NotBlank(message = "Venue name is required")
    @Size(max = 100, message = "Venue name must be less than 100 characters")
    private String name;

    @Size(max = 255, message = "Address must be less than 255 characters")
    private String address;

    private Double latitude;
    private Double longitude;
    private Integer capacity;
    private List<String> facilities;

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
}
