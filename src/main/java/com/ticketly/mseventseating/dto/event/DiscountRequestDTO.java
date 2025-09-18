package com.ticketly.mseventseating.dto.event;

import com.ticketly.mseventseating.model.discount.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRequestDTO {

    private String code;
    private DiscountType type;
    private Object parameters; // JSON representation of DiscountParameters
    private Integer maxUsage;
    private boolean isActive;
    private boolean isPublic;
    private OffsetDateTime activeFrom;
    private OffsetDateTime expiresAt;
    private List<String> applicableTierIds;
}
