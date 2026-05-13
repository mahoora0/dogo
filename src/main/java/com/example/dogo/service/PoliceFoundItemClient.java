package com.example.dogo.service;

import com.example.dogo.dto.PoliceFoundItemDetailResponse;
import com.example.dogo.dto.PoliceFoundItemPage;

import java.time.LocalDate;
import java.util.Optional;

public interface PoliceFoundItemClient {

	PoliceFoundItemPage fetchFoundItems(LocalDate startDate, LocalDate endDate, int pageNo, int numOfRows, String regionCode);

	Optional<PoliceFoundItemDetailResponse> fetchFoundItemDetail(String atcId, Integer fdSn);
}
