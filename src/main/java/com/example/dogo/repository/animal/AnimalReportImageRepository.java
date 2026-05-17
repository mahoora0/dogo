package com.example.dogo.repository.animal;

import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.animal.AnimalReportImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnimalReportImageRepository extends JpaRepository<AnimalReportImage, Long> {

	Optional<AnimalReportImage> findFirstByAnimalReportOrderBySortOrderAscImageIdAsc(AnimalReport animalReport);

	List<AnimalReportImage> findByAnimalReportOrderBySortOrderAscImageIdAsc(AnimalReport animalReport);
}
