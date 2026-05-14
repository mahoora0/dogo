package com.example.dogo.repository;

import com.example.dogo.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByCategoryOrderByRegdateDescInquiryIdDesc(String category);

    List<Inquiry> findAllByOrderByRegdateDescInquiryIdDesc();

    Page<Inquiry> findAllByOrderByRegdateDescInquiryIdDesc(Pageable pageable);

    Page<Inquiry> findByCategoryOrderByRegdateDescInquiryIdDesc(String category, Pageable pageable);

    Page<Inquiry> findByStatusOrderByRegdateDescInquiryIdDesc(String status, Pageable pageable);

    Page<Inquiry> findByStatusNotOrderByRegdateDescInquiryIdDesc(String status, Pageable pageable);

    Page<Inquiry> findByCategoryAndStatusOrderByRegdateDescInquiryIdDesc(String category, String status, Pageable pageable);

    Page<Inquiry> findByCategoryAndStatusNotOrderByRegdateDescInquiryIdDesc(String category, String status, Pageable pageable);
}

