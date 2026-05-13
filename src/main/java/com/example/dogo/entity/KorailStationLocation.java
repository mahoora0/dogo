package com.example.dogo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "korail_station_location", indexes = {
    @Index(name = "idx_station_lookup", columnList = "station_name, line_name")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class KorailStationLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "station_id")
    private Long stationId;

    @Column(name = "line_name", nullable = false, length = 50)
    private String lineName;

    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "exit_count")
    private Integer exitCount;
}
