package com.example.dogo.repository;

import com.example.dogo.entity.LostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LostItemRepository extends JpaRepository<LostItem, Long> {

	@Query("""
			SELECT item
			FROM LostItem item
			WHERE item.deleted = false
				AND (:keyword IS NULL OR :keyword = ''
					OR LOWER(item.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(item.itemName) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(item.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
				AND (:category IS NULL OR :category = '' OR item.categoryMain = :category)
				AND (:area IS NULL OR :area = ''
					OR LOWER(item.lostArea) LIKE LOWER(CONCAT('%', :area, '%'))
					OR LOWER(item.lostPlace) LIKE LOWER(CONCAT('%', :area, '%')))
				AND (:status IS NULL OR :status = '' OR item.status = :status)
			ORDER BY item.lostAt DESC, item.lostId DESC
			""")
	List<LostItem> search(
			@Param("keyword") String keyword,
			@Param("category") String category,
			@Param("area") String area,
			@Param("status") String status
	);
}
