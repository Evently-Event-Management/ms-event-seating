package com.ticketly.mseventseating.repository;


import com.ticketly.mseventseating.model.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, String userId);

    List<OrganizationMember> findByOrganizationId(UUID organizationId);
}
