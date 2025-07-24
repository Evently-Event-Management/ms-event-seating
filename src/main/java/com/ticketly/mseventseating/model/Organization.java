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
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String logoUrl;
    private String website;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    private List<Event> events;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
