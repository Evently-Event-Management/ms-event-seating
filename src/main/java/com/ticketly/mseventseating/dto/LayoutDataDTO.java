package com.ticketly.mseventseating.dto;

import lombok.Data;

import java.util.List;

@Data
public class LayoutDataDTO {
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
        // Fields for structural template
        private Integer rowCount;   // Changed from 'rows' to avoid naming conflict
        private Integer columns;
        private Integer capacity;
        private Integer width;
        private Integer height;
        private String startRowLabel;    // Added for custom row labeling
        private Integer startColumnLabel; // Added for custom column numbering
        // Fields for session map
        private List<Row> rows;     // Keep this as 'rows'
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
