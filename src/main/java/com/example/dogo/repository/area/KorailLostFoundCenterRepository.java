package com.example.dogo.repository.area;

import com.example.dogo.entity.area.KorailLostFoundCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface KorailLostFoundCenterRepository extends JpaRepository<KorailLostFoundCenter, Long> {
    
    @Query("SELECT c.centerId as id, c.stationName as stationName, c.lineName as lineName, " +
           "c.telNo as telNo, c.operatingHours as operatingHours, c.locationDetails as locationDetails, " +
           "c.subRegion as subRegion, " +
           "COALESCE(MAX(s.latitude), 0) as latitude, COALESCE(MAX(s.longitude), 0) as longitude " +
           "FROM KorailLostFoundCenter c " +
           "LEFT JOIN KorailStationLocation s ON " +
           "REPLACE(s.stationName, '역', '') = REPLACE(c.stationName, '역', '') OR " +
           "s.stationName LIKE CONCAT(REPLACE(c.stationName, '역', ''), '(%') OR " +
           "c.stationName LIKE CONCAT(REPLACE(s.stationName, '역', ''), '(%') " +
           "GROUP BY c.centerId, c.stationName, c.lineName, c.telNo, c.operatingHours, c.locationDetails, c.subRegion")
    List<Map<String, Object>> findAllWithCoordinates();
}
