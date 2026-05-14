package com.example.dogo.repository;

import com.example.dogo.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByCategoryOrderByRegdateDescInquiryIdDesc(String category);

    List<Inquiry> findAllByOrderByRegdateDescInquiryIdDesc();
}
