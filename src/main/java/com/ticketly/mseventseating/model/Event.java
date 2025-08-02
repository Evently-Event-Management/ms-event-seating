package com.ticketly.mseventseating.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @ElementCollection
    @CollectionTable(name = "event_cover_photos", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "photo_url")
    private List<String> coverPhotos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.PENDING;

    private String rejectionReason;

    @Column(nullable = false)
    @Builder.Default
    private boolean isOnline = false;

    private String onlineLink;

    private String locationDescription;

    // --- Rolling Sales Window Rules ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SalesStartRuleType salesStartRuleType = SalesStartRuleType.IMMEDIATE;

    @Column(name = "sales_start_days_before")
    private Integer salesStartDaysBefore;

    @Column(name = "sales_start_fixed_datetime")
    private OffsetDateTime salesStartFixedDatetime;
    // --- End of Sales Window Rules ---

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tier> tiers;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_categories",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventSession> sessions;

    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private EventSeatingMap eventSeatingMap;
}