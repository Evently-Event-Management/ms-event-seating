package com.ticketly.mseventseating.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatingLayoutTemplateDTO {
    private UUID id;
    private UUID organizationId;
    private String name;
    private LayoutDataDTO layoutData;
}
