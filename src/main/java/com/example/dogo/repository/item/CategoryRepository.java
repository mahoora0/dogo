package com.example.dogo.repository.item;

import com.example.dogo.entity.item.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	List<Category> findByParentCategoryIdIsNullAndActiveTrueOrderBySortOrderAscCategoryIdAsc();
}
