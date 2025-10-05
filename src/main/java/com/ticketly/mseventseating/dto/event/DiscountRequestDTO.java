package com.ticketly.mseventseating.dto.event;

import dto.projection.discount.DiscountParametersDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRequestDTO {
    private String code;
    private DiscountParametersDTO parameters;
    private Integer maxUsage;
    private boolean isActive;
    private boolean isPublic;
    private OffsetDateTime activeFrom;
    private OffsetDateTime expiresAt;
    private List<UUID> applicableTierIds;
    private List<UUID> applicableSessionIds;
}
