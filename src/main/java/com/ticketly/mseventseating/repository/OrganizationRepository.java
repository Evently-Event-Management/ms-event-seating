package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Find organizations owned by a specific user
     *
     * @param userId the ID of the user
     * @return list of organizations owned by the user
     */
    List<Organization> findByUserId(String userId);

    /**
     * Count organizations owned by a specific user
     *
     * @param userId the ID of the user
     * @return the number of organizations owned by the user
     */
    long countByUserId(String userId);
}
