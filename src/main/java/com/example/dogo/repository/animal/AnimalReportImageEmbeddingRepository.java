package com.example.dogo.repository.animal;

import com.example.dogo.entity.animal.AnimalReportImageEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimalReportImageEmbeddingRepository extends JpaRepository<AnimalReportImageEmbedding, Long> {

	Optional<AnimalReportImageEmbedding> findByReportReportId(Long reportId);

	List<AnimalReportImageEmbedding> findByModelName(String modelName);

	@Query("SELECT e FROM AnimalReportImageEmbedding e WHERE e.report.reportId IN :reportIds")
	List<AnimalReportImageEmbedding> findByReportIds(@Param("reportIds") List<Long> reportIds);

	@Query("SELECT e FROM AnimalReportImageEmbedding e WHERE e.report.reportId IN :reportIds AND e.modelName = :modelName")
	List<AnimalReportImageEmbedding> findByReportIdsAndModelName(
			@Param("reportIds") List<Long> reportIds,
			@Param("modelName") String modelName
	);
}
