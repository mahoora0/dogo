package com.example.dogo.service.animal.api;

import com.example.dogo.dto.animal.AnimalPublicApiPage;

import java.time.LocalDate;

public interface AnimalPublicApiClient {

	AnimalPublicApiPage fetch(LocalDate startDate, LocalDate endDate, int pageNo, int numOfRows);
}
