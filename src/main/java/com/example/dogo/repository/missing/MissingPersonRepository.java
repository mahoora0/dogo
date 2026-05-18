package com.example.dogo.repository.missing;

import com.example.dogo.entity.missing.MissingPersonReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MissingPersonRepository extends JpaRepository<MissingPersonReport, Long>, JpaSpecificationExecutor<MissingPersonReport> {
}
