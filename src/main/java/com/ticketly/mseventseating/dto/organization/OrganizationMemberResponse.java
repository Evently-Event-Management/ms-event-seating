package com.ticketly.mseventseating.dto.organization;

import com.ticketly.mseventseating.model.OrganizationRole;
import lombok.Builder;
import lombok.Data;

// Response for a single organization member
@Data
@Builder
public class OrganizationMemberResponse {
    private String userId;
    private String email;
    private String name;
    private OrganizationRole role;
}