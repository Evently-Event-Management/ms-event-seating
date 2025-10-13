package com.ticketly.mseventseating.dto.organization;

import com.ticketly.mseventseating.model.OrganizationRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

// Response for a single organization member
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationMemberResponse {
    private UUID id;
    private String userId;
    private String email;
    private String name;
    private Set<OrganizationRole> roles; // Changed from a single role to a set of roles
    private boolean isActive;
}