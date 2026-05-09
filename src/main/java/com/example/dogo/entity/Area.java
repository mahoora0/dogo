package com.example.dogo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "AREA")
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Area {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "AREA_ID")
  private Long areaId; // 지역 번호 PK

  @Column(name = "AREA_NAME", nullable = false)
  private String areaName; // 지역명

  @Column(name = "AREA_CODE")
  private String areaCode; // 지역 코드

  @Column(name = "PARENT_AREA_ID")
  private Long parentAreaId; // 상위 지역 FK

  @Column(name = "SORT_ORDER")
  private int sortOrder; // 정렬 순서

  @Column(name = "IS_ACTIVE")
  private boolean isActive = true; // 활성 여부

  @Column(name = "LATITUDE")
  private Double latitude; // 위도

  @Column(name = "LONGITUDE")
  private Double longitude; // 경도

  @Column(name = "DEFAULT_LEVEL")
  private Integer defaultLevel; // 기본 확대 레벨
}
