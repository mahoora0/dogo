package com.example.dogo.service;

import com.example.dogo.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public List<String> getActiveCategoryNames() {
		return categoryRepository.findByParentCategoryIdIsNullAndActiveTrueOrderBySortOrderAscCategoryIdAsc().stream()
				.map(category -> category.getCategoryName())
				.toList();
	}
}
