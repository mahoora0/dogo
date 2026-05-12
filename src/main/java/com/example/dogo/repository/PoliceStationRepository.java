package com.example.dogo.repository;

import com.example.dogo.entity.PoliceStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PoliceStationRepository extends JpaRepository<PoliceStation, String> {

  List<PoliceStation> findByAddrStartingWith(String region);
}