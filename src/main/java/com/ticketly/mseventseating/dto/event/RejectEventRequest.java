package com.ticketly.mseventseating.dto.event;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RejectEventRequest {

    @NotBlank(message = "Rejection reason cannot be blank")
    private String reason;
}
