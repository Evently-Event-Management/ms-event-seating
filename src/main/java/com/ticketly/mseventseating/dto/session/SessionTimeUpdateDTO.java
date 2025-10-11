package com.ticketly.mseventseating.dto.session;

import com.ticketly.mseventseating.validators.ValidSalesStartTime;
import com.ticketly.mseventseating.validators.ValidSessionDuration;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidSessionDuration(minMinutes = 30, maxHours = 12)
@ValidSalesStartTime
public class SessionTimeUpdateDTO {
    
    @NotNull
    @Future
    private OffsetDateTime startTime;

    @NotNull
    @Future
    private OffsetDateTime endTime;

    @NotNull
    private OffsetDateTime salesStartTime;
}