package com.example.dogo.repository;

import com.example.dogo.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

  // 성화 상태인 지역 목록을 정렬 순서에 따라 조회
  List<Area> findByIsActiveTrueOrderBySortOrderAsc();

  // 특정 상위 지역(PARENT_AREA_ID)에 속한 하위 지역 목록을 조회
  List<Area> findByParentAreaIdOrderBySortOrderAsc(Long parentAreaId);

  //지역 코드를 통해 특정 지역 정보를 조회
  Area findByAreaCode(String areaCode);
}
