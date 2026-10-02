package org.tomo.beton.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MarketPlaceDto {
    private Long id;
    private String name;
    private String address;
    private MarketPlaceType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private String notes;
}
