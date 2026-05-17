package com.example.dogo.service.police.client;

import com.example.dogo.dto.police.PoliceLostItemPage;
import com.example.dogo.dto.police.PoliceLostItemDetailResponse;

import java.time.LocalDate;
import java.util.Optional;

public interface PoliceLostItemClient {

	PoliceLostItemPage fetchLostItems(LocalDate startDate, LocalDate endDate, int pageNo, int numOfRows);

	Optional<PoliceLostItemDetailResponse> fetchLostItemDetail(String atcId);
}
