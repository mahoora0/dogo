package com.example.dogo.repository.item;

import com.example.dogo.entity.item.LostItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import com.example.dogo.entity.user.User;
public interface LostItemRepository extends JpaRepository<LostItem, Long>, JpaSpecificationExecutor<LostItem> {


	// 특정 사용자의 등록한 분실물 중, 삭제되지 않고 활성 상태인 목록을 조회합니다.
	List<LostItem> findByUserAndDeletedFalseAndStatusIn(User user, List<String> statuses);

	// 특정 사용자가 등록한 분실물(삭제되지 않은 것)이 최소 1개 이상 존재하는지 여부를 확인합니다.
	boolean existsByUserAndDeletedFalse(User user);

	boolean existsByAtcId(String atcId);

	boolean existsBySourceType(String sourceType);

	@Query("""
			SELECT item
			FROM LostItem item
			WHERE item.deleted = false
				AND item.status = 'WAITING'
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

	List<LostItem> findByUserAndDeletedFalseOrderByRegDateDesc(com.example.dogo.entity.user.User user);

	List<LostItem> findByDeletedFalseOrderByLostAtDescLostIdDesc(Pageable pageable);

	List<LostItem> findByDeletedFalseOrderByRegDateDescLostIdDesc(Pageable pageable);

	List<LostItem> findByDeletedFalseOrderByRegDateDesc();

	Page<LostItem> findByDeletedFalseOrderByRegDateDesc(Pageable pageable);

	List<LostItem> findBySourceTypeAndDeletedFalseOrderByRegDateDesc(String sourceType);

	Page<LostItem> findBySourceTypeAndDeletedFalseOrderByRegDateDesc(String sourceType, Pageable pageable);

	long countByDeletedFalseAndCategoryMainIn(List<String> categories);

	long countByDeletedFalse();
}
