package com.ticketly.mseventseating.model;

import jakarta.persistence.*;
import lombok.*;
import model.SessionStatus;
import model.SessionType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.PENDING;

    // --- Session-Specific Location & Sales Rules ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType sessionType;

    // ✅ ADDED: A JSONB column to store embedded physical venue details.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "venue_details", columnDefinition = "jsonb")
    private String venueDetails; // Stores a JSON string of a VenueDetailsDTO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SalesStartRuleType salesStartRuleType = SalesStartRuleType.IMMEDIATE;

    @Column(name = "sales_start_hours_before")
    private Integer salesStartHoursBefore;

    @Column(name = "sales_start_fixed_datetime")
    private OffsetDateTime salesStartFixedDatetime;

    @OneToOne(mappedBy = "eventSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private SessionSeatingMap sessionSeatingMap;
}
