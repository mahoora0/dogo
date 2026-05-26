package com.example.dogo.repository.item;

import com.example.dogo.entity.item.ItemMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

	Optional<ItemMatch> findByLostItemLostIdAndFoundItemFoundId(Long lostId, Long foundId);

	void deleteByLostItemLostId(Long lostId);

	void deleteByFoundItemFoundId(Long foundId);

	@Modifying
	@Query("DELETE FROM ItemMatch m WHERE m.lostItem.lostId = :lostId AND m.foundItem.foundId NOT IN :foundIds")
	void deleteByLostItemLostIdAndFoundItemFoundIdNotIn(@Param("lostId") Long lostId, @Param("foundIds") List<Long> foundIds);

	@Modifying
	@Query("DELETE FROM ItemMatch m WHERE m.foundItem.foundId = :foundId AND m.lostItem.lostId NOT IN :lostIds")
	void deleteByFoundItemFoundIdAndLostItemLostIdNotIn(@Param("foundId") Long foundId, @Param("lostIds") List<Long> lostIds);

	void deleteByLostItemUser(com.example.dogo.entity.user.User user);

	void deleteByFoundItemUser(com.example.dogo.entity.user.User user);

	// 배지 숫자용: 아직 읽지 않은(CANDIDATE) 매칭 건수 조회
	long countByLostItemUserUserNoAndMatchStatus(Long userNo, String matchStatus);

	// 배지 숫자 클릭 후 읽음 처리용: CANDIDATE 상태 상위 3개 조회
	List<ItemMatch> findTop3ByLostItemUserUserNoAndMatchStatusOrderByFinalScoreDescMatchIdDesc(Long userNo, String matchStatus);

	// 드롭다운 내용 표시용: 읽음 여부 상관없이 점수 상위 3개 조회 (클릭 후에도 내용 유지됨)
	List<ItemMatch> findTop3ByLostItemUserUserNoOrderByFinalScoreDescMatchIdDesc(Long userNo);

	@Modifying
	@Query("UPDATE ItemMatch m SET m.matchStatus = 'READ', m.moddate = CURRENT_TIMESTAMP WHERE m.lostItem.user.userNo = :userNo AND m.matchStatus = 'CANDIDATE'")
	void markAllAsReadByUserNo(@Param("userNo") Long userNo);
}
