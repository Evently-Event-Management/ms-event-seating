package com.ticketly.mseventseating.dto.event;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectEventRequest {
    @NotBlank
    private String reason;
}
