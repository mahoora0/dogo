package com.example.dogo.service.police.client;

import com.example.dogo.dto.police.PoliceRegionCode;

import java.util.List;

public interface PoliceCommonCodeClient {

	List<PoliceRegionCode> fetchRegionCodes();
}
