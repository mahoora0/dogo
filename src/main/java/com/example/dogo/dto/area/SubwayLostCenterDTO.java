package com.example.dogo.dto.area;

import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubwayLostCenterDTO {

  private Long id;
  private String operatorName;
  private String lineName;
  private String region;
  private String subRegion;
  @Column(name = "station_name")
  private String stationName;
  private Double latitude;
  private Double longitude;
  private String detailLocation;
  private String availableTime;
  private String telNo;
}
