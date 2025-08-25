package com.ticketly.mseventseating.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "organization_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonBackReference("organization-members") // This is the "child" side
    private Organization organization;

    @Column(name = "user_id", nullable = false)
    private String userId; // The user's ID from Keycloak

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "organization_member_roles",
        joinColumns = @JoinColumn(name = "organization_member_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Set<OrganizationRole> roles = new HashSet<>();
}
