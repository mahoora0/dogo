package com.example.dogo.repository.animal;

import com.example.dogo.entity.animal.AnimalReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnimalReportRepository extends JpaRepository<AnimalReport, Long> {

	@Query("""
			SELECT report
			FROM AnimalReport report
			WHERE report.deleted = false
				AND (:reportType IS NULL OR :reportType = '' OR report.reportType = :reportType)
				AND (:animalType IS NULL OR :animalType = '' OR report.animalType = :animalType)
				AND (:region IS NULL OR :region = '' OR report.regionName = :region)
				AND (:keyword IS NULL OR :keyword = ''
					OR LOWER(report.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(report.breedName) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(report.furColor) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(report.distinctiveMarks) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(report.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(report.detailPlace) LIKE LOWER(CONCAT('%', :keyword, '%')))
			""")
	Page<AnimalReport> search(
			@Param("reportType") String reportType,
			@Param("animalType") String animalType,
			@Param("region") String region,
			@Param("keyword") String keyword,
			Pageable pageable
	);
}
