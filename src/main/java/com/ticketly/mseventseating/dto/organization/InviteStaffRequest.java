package com.ticketly.mseventseating.dto.organization;

import com.ticketly.mseventseating.model.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

// Request to invite a new member
@Data
public class InviteStaffRequest {
    @NotBlank
    @Email
    private String email;

    @NotEmpty
    private Set<OrganizationRole> roles; // Now supporting multiple roles
}
