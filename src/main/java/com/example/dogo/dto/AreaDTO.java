package com.example.dogo.dto;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
public class AreaDTO {

  private Long areaId;
  private String areaName;
  private String areaCode;
  private Long parentAreaId;
  private int sortOrder;
  private boolean isActive;
  private Double latitude;
  private Double longitude;
  private Integer defaultLevel;

}
