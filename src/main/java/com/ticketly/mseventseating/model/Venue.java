package com.ticketly.mseventseating.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "venues")  // Added table name explicitly
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String address; // Added for completeness

    private Double latitude;
    private Double longitude;

    private Integer capacity;

    @ElementCollection
    @CollectionTable(name = "venue_facilities", joinColumns = @JoinColumn(name = "venue_id"))
    private List<String> facilities;

    // An organization owns the venue
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // REMOVED: hasSeats
    // REMOVED: layoutJson
}