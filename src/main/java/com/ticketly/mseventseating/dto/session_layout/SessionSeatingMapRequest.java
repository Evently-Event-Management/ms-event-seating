package com.ticketly.mseventseating.dto.session_layout;

import lombok.Data;

import java.util.List;

@Data
public class SessionSeatingMapRequest {
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

        // ✅ The frontend will now send the fully expanded rows and seats.
        // The backend's only job is to process this structure.
        private List<Row> rows;

        // Fields for non-seated blocks
        private Integer capacity;
        private Integer width;
        private Integer height;
        private Integer soldCount;
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
        private String status;
    }

    @Data
    public static class Position {
        private Double x;
        private Double y;
    }
}
