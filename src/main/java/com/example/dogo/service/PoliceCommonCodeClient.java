package com.example.dogo.service;

import com.example.dogo.dto.PoliceRegionCode;

import java.util.List;

public interface PoliceCommonCodeClient {

	List<PoliceRegionCode> fetchRegionCodes();
}
