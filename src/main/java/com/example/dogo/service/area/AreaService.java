package com.example.dogo.service.area;

import com.example.dogo.dto.area.AreaDTO;
import com.example.dogo.entity.area.Area;
import com.example.dogo.repository.area.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AreaService {

  private final AreaRepository areaRepository;

  public Long saveArea(AreaDTO areaDTO) { //지역 정보 저장 (Create/Update)
    // DTO를 Entity로 변환
    Area area = Area.builder()
        .areaId(areaDTO.getAreaId())
        .areaName(areaDTO.getAreaName())
        .areaCode(areaDTO.getAreaCode())
        .parentAreaId(areaDTO.getParentAreaId())
        .sortOrder(areaDTO.getSortOrder())
        .isActive(areaDTO.isActive())
        .latitude(areaDTO.getLatitude())
        .longitude(areaDTO.getLongitude())
        .defaultLevel(areaDTO.getDefaultLevel())
        .build();
    return areaRepository.save(area).getAreaId();
  }

  @Transactional(readOnly = true)
  public List<AreaDTO> getActiveAreas() { // 활성화된 전체 지역 목록 조회 (Read)
    // DB에서 활성 상태인 지역만 가져와 DTO 리스트로 변환
    return areaRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
        .map(this::entityToDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public AreaDTO getAreaById(Long areaId) { // 특정 지역 상세 조회
    Area area = areaRepository.findById(areaId)
        .orElseThrow(() -> new IllegalArgumentException("해당 지역이 존재하지 않습니다. ID: " + areaId));
    return entityToDto(area);
  }

  private AreaDTO entityToDto(Area area) { // Entity를 DTO로 변환하는 내부 메서드
    return AreaDTO.builder()
        .areaId(area.getAreaId())
        .areaName(area.getAreaName())
        .areaCode(area.getAreaCode())
        .parentAreaId(area.getParentAreaId())
        .sortOrder(area.getSortOrder())
        .isActive(area.isActive())
        .latitude(area.getLatitude())
        .longitude(area.getLongitude())
        .defaultLevel(area.getDefaultLevel())
        .build();
  }
}