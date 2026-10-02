package org.tomo.beton.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tomo.beton.dtos.MarketPlaceType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "marketplaces")
public class MarketPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private MarketPlaceType type;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
