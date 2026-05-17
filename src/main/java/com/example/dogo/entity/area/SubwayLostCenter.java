package com.example.dogo.entity.area;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subway_lost_center")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubwayLostCenter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "operator_name")
  private String operatorName;

  @Column(name = "line_name")
  private String lineName;

  @Column(name = "region")
  private String region;

  @Column(name = "sub_region")
  private String subRegion;

  @Column(name = "station_name", nullable = false)
  private String stationName;

  private Double latitude;
  private Double longitude;

  @Column(name = "detail_location")
  private String detailLocation;

  @Column(name = "available_time")
  private String availableTime;

  @Column(name = "tel_no")
  private String telNo;
}