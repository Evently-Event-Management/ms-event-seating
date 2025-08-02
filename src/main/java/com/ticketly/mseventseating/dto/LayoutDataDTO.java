package com.ticketly.mseventseating.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LayoutDataDTO {
    private String name;
    private Layout layout;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Layout {
        private List<Block> blocks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Block {
        private String id;
        private String name;
        private String type;
        private Position position;

        // For seated_grid blocks
        private Integer rows;
        private Integer columns;
        private String startRowLabel;
        private Integer startColumnLabel;

        // For non_sellable and standing_capacity blocks
        private Integer width;
        private Integer height;

        // For standing_capacity blocks
        private Integer capacity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private Double x;
        private Double y;
    }
}
