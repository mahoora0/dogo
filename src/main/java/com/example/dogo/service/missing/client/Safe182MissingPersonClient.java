package com.example.dogo.service.missing.client;

public interface Safe182MissingPersonClient {

	Safe182MissingPersonPage search(String keyword, int page, int rowSize);

	Safe182MissingPersonPage searchByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate, int page, int rowSize);
}
