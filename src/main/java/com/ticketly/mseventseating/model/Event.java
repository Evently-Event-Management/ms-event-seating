package com.ticketly.mseventseating.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2048)
    private String description;

    @ElementCollection
    @CollectionTable(name = "event_cover_photos", joinColumns = @JoinColumn(name = "event_id"))
    private List<String> coverPhotos;

    @Column(length = 4096)
    private String overview;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private boolean isOnline;

    private String onlineLink;

    private String locationDescription;

    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.PENDING;

    private String rejectionReason;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<Tier> tiers;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<SeatMap> seatMaps;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters and setters
}