package com.repcheck.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "daily_load")
@Getter
@Setter
@NoArgsConstructor
public class DailyLoad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "load_date", nullable = false)
    private LocalDate loadDate;

    @Column(name = "total_volume_kg", nullable = false)
    private double totalVolumeKg;

    @Column(nullable = false)
    private double atl;

    @Column(nullable = false)
    private double ctl;

    @Column(nullable = false)
    private double tsb;
}
