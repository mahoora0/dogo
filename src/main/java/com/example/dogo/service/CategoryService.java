package com.example.dogo.service;

import com.example.dogo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;

	@Transactional(readOnly = true)
	public List<String> getActiveCategoryNames() {
		return categoryRepository.findByParentCategoryIdIsNullAndActiveTrueOrderBySortOrderAscCategoryIdAsc().stream()
				.map(category -> category.getCategoryName())
				.toList();
	}
}
