package com.example.dogo.repository.animal;

import com.example.dogo.entity.animal.AnimalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AnimalReportRepository extends JpaRepository<AnimalReport, Long>, JpaSpecificationExecutor<AnimalReport> {

	void deleteByUser(com.example.dogo.entity.user.User user);

	@Query("""
		SELECT r FROM AnimalReport r
		WHERE r.reportType = 'SIGHTING'
		AND r.animalType = :animalType
		AND r.deleted = false
		AND r.status IN ('OPEN', 'MATCHING')
		AND r.eventDate BETWEEN :from AND :to
		""")
	List<AnimalReport> findSightingCandidates(
			@Param("animalType") String animalType,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to
	);

	@Query("""
		SELECT r FROM AnimalReport r
		WHERE r.reportType = 'MISSING'
		AND r.animalType = :animalType
		AND r.deleted = false
		AND r.status IN ('OPEN', 'MATCHING')
		AND r.eventDate BETWEEN :from AND :to
		""")
	List<AnimalReport> findMissingCandidates(
			@Param("animalType") String animalType,
			@Param("from") LocalDate from,
			@Param("to") LocalDate to
	);
	List<AnimalReport> findByDeletedFalseOrderByRegdateDesc();
	List<AnimalReport> findByUserAndDeletedFalseOrderByRegdateDesc(com.example.dogo.entity.user.User user);
}
