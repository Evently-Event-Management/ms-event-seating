package com.ticketly.mseventseating.dto.event;

import lombok.Data;

import java.util.List;

@Data
public class SessionSeatingMapDTO {
    private String name;
    private Layout layout;

    @Data
    public static class Layout {
        private List<Block> blocks;
    }

    @Data
    public static class Block {
        private String id;
        private String name;
        private String type;
        private Position position;

        // --- Fields for 'seated_grid' type ---
        private List<Row> rows;

        // --- Fields for 'standing_capacity' type ---
        private Integer capacity;
        // ✅ ADDED: A flat list of seats for capacity-based blocks (online or GA)
        private List<Seat> seats;

        // --- Fields for resizable blocks ---
        private Integer width;
        private Integer height;

        // --- Fields used AFTER transformation on the backend ---
        private Integer soldCount; // This will be calculated from seat statuses
        private String tierId;
    }

    @Data
    public static class Row {
        private String id;
        private String label;
        private List<Seat> seats;
    }

    @Data
    public static class Seat {
        private String id;
        private String label;
        private String tierId;
        // Status can be 'AVAILABLE', 'RESERVED', 'BOOKED', etc.
        // The backend will validate the initial state.
        private String status;
    }

    @Data
    public static class Position {
        private Double x;
        private Double y;
    }
}
