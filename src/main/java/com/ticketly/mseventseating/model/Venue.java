package com.ticketly.mseventseating.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
public class Venue {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    private Double latitude;
    private Double longitude;
    private Integer capacity;

    private boolean hasSeats;

    @ElementCollection
    @CollectionTable(name = "venue_facilities", joinColumns = @JoinColumn(name = "venue_id"))
    private List<String> facilities;

    @Lob
    @Column(name = "layout_json", columnDefinition = "jsonb")
    private String layoutJson;

    // getters and setters
}
