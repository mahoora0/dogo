package com.example.dogo.repository.missing;

import com.example.dogo.entity.missing.MissingPersonReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface MissingPersonRepository extends JpaRepository<MissingPersonReport, Long>, JpaSpecificationExecutor<MissingPersonReport> {

	List<MissingPersonReport> findByDeletedFalseOrderByRegdateDesc();

	Optional<MissingPersonReport> findByApiProviderAndExternalId(String apiProvider, String externalId);

	List<MissingPersonReport> findByUserAndDeletedFalseOrderByRegdateDesc(com.example.dogo.entity.user.User user);

	boolean existsBySourceType(String sourceType);

	boolean existsByApiProviderAndExternalId(String apiProvider, String externalId);
}
