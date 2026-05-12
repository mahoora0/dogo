package com.example.dogo.repository;

import com.example.dogo.entity.FoundItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoundItemRepository extends JpaRepository<FoundItem, Long> {

	boolean existsByAtcIdAndFdSn(String atcId, Integer fdSn);

	boolean existsBySourceType(String sourceType);

	@Query("""
			SELECT item
			FROM FoundItem item
			WHERE item.deleted = false
				AND (:keyword IS NULL OR :keyword = ''
					OR LOWER(item.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(item.itemName) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(item.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
				AND (:category IS NULL OR :category = '' OR item.categoryMain = :category)
				AND (:area IS NULL OR :area = ''
					OR LOWER(item.foundArea) LIKE LOWER(CONCAT('%', :area, '%'))
					OR LOWER(item.foundPlace) LIKE LOWER(CONCAT('%', :area, '%')))
				AND (:status IS NULL OR :status = '' OR item.status = :status)
			""")
	Page<FoundItem> search(
			@Param("keyword") String keyword,
			@Param("category") String category,
			@Param("area") String area,
			@Param("status") String status,
			Pageable pageable
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
}
