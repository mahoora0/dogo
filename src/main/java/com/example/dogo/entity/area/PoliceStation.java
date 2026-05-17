package com.example.dogo.entity.area;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "police_station")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PoliceStation {

  @Id
  @Column(length = 19)
  private String pnu;

  private Double longitude;
  private Double latitude;

  @Column(length = 1000)
  private String address;

  @Column(length = 1000)
  private String address1;

  @Column(name = "no_value")
  private Integer noValue;

  @Column(length = 100)
  private String lclsf;

  @Column(name = "cmptnc_rgn_nm", length = 255)
  private String cmptncRgnNm;

  @Column(name = "polstn_nm", length = 255)
  private String polstnNm;

  @Column(length = 100)
  private String se;

  @Column(length = 100)
  private String telno;

  @Column(length = 1000)
  private String addr;
}