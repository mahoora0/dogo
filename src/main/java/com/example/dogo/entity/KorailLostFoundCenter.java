package com.example.dogo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "korail_lost_found_center", indexes = {
    @Index(name = "idx_lost_found_station", columnList = "station_name, line_name")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class KorailLostFoundCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "center_id")
    private Long centerId;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(name = "line_name", length = 50)
    private String lineName;

    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;

    @Column(name = "location_details", length = 500)
    private String locationDetails;

    @Column(name = "operating_hours", length = 200)
    private String operatingHours;

    @Column(name = "tel_no", length = 50)
    private String telNo;
}
