package com.ticketly.mseventseating.dto.organization;

import com.ticketly.mseventseating.model.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Request to invite a new member
@Data
public class InviteStaffRequest {
    @NotBlank
    @Email
    private String email;

    @NotNull
    private OrganizationRole role; // e.g., "ADMIN" or "MEMBER"
}
