package com.ticketly.mseventseating.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_sessions")  // Changed from "event_session" to "event_sessions"
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
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SalesStartRuleType salesStartRuleType = SalesStartRuleType.IMMEDIATE;

    @Column(name = "sales_start_hours_before")
    private Integer salesStartHoursBefore;

    @Column(name = "sales_start_fixed_datetime")
    private OffsetDateTime salesStartFixedDatetime;
    // --- End of Sales Window Rules ---

    // ✅ ADDED: A one-to-one mapping to this session's specific seating map.
    @OneToOne(mappedBy = "eventSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private SessionSeatingMap sessionSeatingMap;
}