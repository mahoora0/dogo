package com.example.dogo.repository.animal;

import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.animal.AnimalReportMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnimalReportMatchRepository extends JpaRepository<AnimalReportMatch, Long> {

	boolean existsByMissingReportAndSightingReport(AnimalReport missingReport, AnimalReport sightingReport);

	@Query("""
		SELECT m FROM AnimalReportMatch m
		JOIN FETCH m.sightingReport
		WHERE m.missingReport.reportId = :reportId
		ORDER BY m.finalScore DESC
		""")
	List<AnimalReportMatch> findByMissingReportId(@Param("reportId") Long reportId);

	@Query("""
		SELECT m FROM AnimalReportMatch m
		JOIN FETCH m.missingReport
		WHERE m.sightingReport.reportId = :reportId
		ORDER BY m.finalScore DESC
		""")
	List<AnimalReportMatch> findBySightingReportId(@Param("reportId") Long reportId);
}
