package com.ticketly.mseventseating.dto.session;

import com.ticketly.mseventseating.dto.session_layout.SessionSeatingMapRequest;
import com.ticketly.mseventseating.model.SalesStartRuleType;
import lombok.Data;
import lombok.Builder;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@Data
@Builder
public class SessionRequest {
    @NotNull
    @Future
    private OffsetDateTime startTime;
    @NotNull
    @Future
    private OffsetDateTime endTime;
    @NotNull
    private SalesStartRuleType salesStartRuleType;
    private Integer salesStartHoursBefore;
    private OffsetDateTime salesStartFixedDatetime;

    // ✅ ADDED: Each session now carries its own layout data.
    @NotNull
    private SessionSeatingMapRequest sessionSeatingMapRequest;
}
