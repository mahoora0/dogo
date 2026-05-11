package com.example.dogo.service;

import com.example.dogo.dto.PoliceLostItemPage;
import com.example.dogo.dto.PoliceLostItemDetailResponse;

import java.time.LocalDate;
import java.util.Optional;

public interface PoliceLostItemClient {

	PoliceLostItemPage fetchLostItems(LocalDate startDate, LocalDate endDate, int pageNo, int numOfRows);

	Optional<PoliceLostItemDetailResponse> fetchLostItemDetail(String atcId);
}
