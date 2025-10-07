package com.ticketly.mseventseating.dto.session;

import dto.SessionSeatingMapDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating just the layout data of a session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionLayoutUpdateDTO {
    
    @NotNull
    private SessionSeatingMapDTO layoutData;
}
