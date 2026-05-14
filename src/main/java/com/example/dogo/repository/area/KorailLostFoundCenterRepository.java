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
           "MAX(s.latitude) as latitude, MAX(s.longitude) as longitude " +
           "FROM KorailLostFoundCenter c " +
           "LEFT JOIN KorailStationLocation s ON TRIM(s.stationName) = TRIM(c.stationName) " +
           "OR s.stationName LIKE CONCAT('%', c.stationName, '%') " +
           "OR c.stationName LIKE CONCAT('%', s.stationName, '%') " +
           "GROUP BY c.centerId, c.stationName, c.lineName, c.telNo, c.operatingHours, c.locationDetails")
    List<Map<String, Object>> findAllWithCoordinates();
}
