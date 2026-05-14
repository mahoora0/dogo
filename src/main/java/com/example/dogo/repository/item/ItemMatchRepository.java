package com.example.dogo.repository.item;

import com.example.dogo.entity.item.ItemMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemMatchRepository extends JpaRepository<ItemMatch, Long> {

	@Query("""
			SELECT m FROM ItemMatch m
			JOIN FETCH m.foundItem
			WHERE m.lostItem.lostId = :lostId
			ORDER BY m.finalScore DESC, m.matchScore DESC
			""")
	List<ItemMatch> findByLostIdWithFoundItem(@Param("lostId") Long lostId);

	@Query("""
			SELECT m FROM ItemMatch m
			JOIN FETCH m.lostItem
			WHERE m.foundItem.foundId = :foundId
			ORDER BY m.finalScore DESC, m.matchScore DESC
			""")
	List<ItemMatch> findByFoundIdWithLostItem(@Param("foundId") Long foundId);

	boolean existsByLostItemLostIdAndFoundItemFoundId(Long lostId, Long foundId);

	void deleteByLostItemLostId(Long lostId);

	void deleteByFoundItemFoundId(Long foundId);
}
