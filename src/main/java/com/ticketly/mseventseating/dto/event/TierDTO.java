package com.ticketly.mseventseating.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TierDTO {
    private UUID id;
    private String name;
    private String color;
    private BigDecimal price;
}
