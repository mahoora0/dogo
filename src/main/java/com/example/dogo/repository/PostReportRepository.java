package com.example.dogo.repository;

import com.example.dogo.entity.PostReport;
import com.example.dogo.entity.ReportTargetType;
import com.example.dogo.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PostReportRepository extends JpaRepository<PostReport, Long>, JpaSpecificationExecutor<PostReport> {

	boolean existsByReporterUserNoAndTargetTypeAndTargetId(Long reporterNo, ReportTargetType targetType, Long targetId);

	long countByTargetOwnerNoAndStatusNot(Long targetOwnerNo, ReportStatus status);
}
