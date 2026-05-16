package com.example.dogo.repository.item;

import com.example.dogo.entity.item.LostItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LostItemRepository extends JpaRepository<LostItem, Long>, JpaSpecificationExecutor<LostItem> {

	boolean existsByAtcId(String atcId);

	boolean existsBySourceType(String sourceType);

	@Query("""
			SELECT item
			FROM LostItem item
			WHERE item.deleted = false
				AND item.status IN ('WAITING', 'MATCHING')
				AND item.lostAt BETWEEN :lostFrom AND :lostTo
				AND (:category IS NULL OR :category = ''
					OR item.categoryMain IS NULL
					OR item.categoryMain = :category)
			ORDER BY item.lostAt DESC, item.lostId DESC
			""")
	List<LostItem> findMatchCandidatesForFound(
			@Param("category") String category,
			@Param("lostFrom") LocalDateTime lostFrom,
			@Param("lostTo") LocalDateTime lostTo
	);

	@Query("""
			SELECT DISTINCT item.categoryMain
			FROM LostItem item
			WHERE item.deleted = false
				AND item.categoryMain IS NOT NULL
				AND item.categoryMain <> ''
			ORDER BY item.categoryMain ASC
			""")
	List<String> findActiveCategoryNames();

	void deleteByUser(com.example.dogo.entity.user.User user);

	List<LostItem> findByDeletedFalseOrderByLostAtDescLostIdDesc(Pageable pageable);
}
