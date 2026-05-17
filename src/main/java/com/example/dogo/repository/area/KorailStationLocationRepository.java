package com.example.dogo.repository.area;

import com.example.dogo.entity.area.KorailStationLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KorailStationLocationRepository extends JpaRepository<KorailStationLocation, Long> {
    Optional<KorailStationLocation> findByStationNameAndLineName(String stationName, String lineName);
}
