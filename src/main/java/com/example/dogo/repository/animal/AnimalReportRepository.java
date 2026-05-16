package com.example.dogo.repository.animal;

import com.example.dogo.entity.animal.AnimalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AnimalReportRepository extends JpaRepository<AnimalReport, Long>, JpaSpecificationExecutor<AnimalReport> {
}
