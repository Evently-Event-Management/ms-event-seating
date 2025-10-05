package com.ticketly.mseventseating.dto.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResponseDTO extends DiscountRequestDTO {
    private UUID id;
    private Integer currentUsage;
}
