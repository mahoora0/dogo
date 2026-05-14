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

  private String operatorName;
  private String lineName;
  @Column(name = "region")
  private String region;

  @Column(name = "sub_region")
  private String subRegion;
  private String stationName;
  private Double latitude;
  private Double longitude;
  private String detailLocation;
  private String availableTime;
  private String telNo;
}