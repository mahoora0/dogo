package com.example.dogo.repository.item;

import com.example.dogo.entity.item.FoundItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FoundItemRepository extends JpaRepository<FoundItem, Long>, JpaSpecificationExecutor<FoundItem> {

	boolean existsByAtcIdAndFdSn(String atcId, Integer fdSn);

	boolean existsBySourceType(String sourceType);

	@Query("""
			SELECT item
			FROM FoundItem item
			WHERE item.deleted = false
				AND item.status IN ('KEEPING', 'MATCHING')
				AND item.foundAt BETWEEN :foundFrom AND :foundTo
				AND (:category IS NULL OR :category = ''
					OR item.categoryMain IS NULL
					OR item.categoryMain = :category)
			ORDER BY item.foundAt ASC, item.foundId ASC
			""")
	List<FoundItem> findMatchCandidatesForLost(
			@Param("category") String category,
			@Param("foundFrom") LocalDateTime foundFrom,
			@Param("foundTo") LocalDateTime foundTo
	);

	@Query("""
			SELECT DISTINCT item.categoryMain
			FROM FoundItem item
			WHERE item.deleted = false
				AND item.categoryMain IS NOT NULL
				AND item.categoryMain <> ''
			ORDER BY item.categoryMain ASC
			""")
	List<String> findActiveCategoryNames();

	void deleteByUser(com.example.dogo.entity.user.User user);

	List<FoundItem> findByDeletedFalseOrderByFoundAtDescFoundIdDesc(Pageable pageable);
}
