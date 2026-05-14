package com.example.dogo.repository.area;

import com.example.dogo.entity.area.PoliceStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PoliceStationRepository extends JpaRepository<PoliceStation, String> {

  List<PoliceStation> findByAddrContainingOrCmptncRgnNmContaining(String addr, String rgn);
  
  List<PoliceStation> findByPnuStartingWith(String prefix);

  @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.cmptncRgnNm FROM PoliceStation p WHERE p.pnu LIKE :prefix% ORDER BY p.cmptncRgnNm")
  List<String> findDistinctCmptncRgnNmByPnuStartingWith(String prefix);
}