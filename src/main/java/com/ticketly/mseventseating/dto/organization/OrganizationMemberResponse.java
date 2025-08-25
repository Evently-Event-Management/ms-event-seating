package com.ticketly.mseventseating.dto.organization;

import com.ticketly.mseventseating.model.OrganizationRole;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

// Response for a single organization member
@Data
@Builder
public class OrganizationMemberResponse {
    private String userId;
    private String email;
    private String name;
    private Set<OrganizationRole> roles; // Changed from a single role to a set of roles
}