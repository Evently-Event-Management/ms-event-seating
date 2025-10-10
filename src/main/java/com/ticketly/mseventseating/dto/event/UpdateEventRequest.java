package com.ticketly.mseventseating.dto.event;

import java.util.UUID;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating basic event details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {
    
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;
    
    private String description;
    
    private String overview;

    private UUID categoryId;
}