package com.ticketly.mseventseating.dto.session;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.SessionStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusUpdateDTO {
    
    @NotNull
    private SessionStatus status;
}