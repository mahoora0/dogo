package com.example.dogo.repository.missing;

import com.example.dogo.entity.missing.MissingPersonImage;
import com.example.dogo.entity.missing.MissingPersonReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MissingPersonImageRepository extends JpaRepository<MissingPersonImage, Long> {

	Optional<MissingPersonImage> findFirstByReportOrderBySortOrderAscImageIdAsc(MissingPersonReport report);

	List<MissingPersonImage> findByReportOrderBySortOrderAscImageIdAsc(MissingPersonReport report);

	List<MissingPersonImage> findByReportInOrderBySortOrderAscImageIdAsc(List<MissingPersonReport> reports);
}
