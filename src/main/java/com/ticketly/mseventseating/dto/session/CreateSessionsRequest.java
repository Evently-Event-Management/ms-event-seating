package com.ticketly.mseventseating.dto.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateSessionsRequest {
    @NotNull
    private UUID eventId;
    
    @Valid
    @NotEmpty
    private List<SessionCreationDTO> sessions;
}