package com.ticketly.mseventseating.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.SessionStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionStatusCountDTO {
    private SessionStatus status;
    private Long count;
}