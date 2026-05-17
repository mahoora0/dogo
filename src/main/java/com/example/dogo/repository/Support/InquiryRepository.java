package com.example.dogo.repository.Support;

import com.example.dogo.entity.Support.Inquiry;
import com.example.dogo.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByCategoryOrderByRegdateDescInquiryIdDesc(String category);

    List<Inquiry> findAllByOrderByRegdateDescInquiryIdDesc();

    // 관리자용 전체 조회
    Page<Inquiry> findAllByOrderByRegdateDescInquiryIdDesc(Pageable pageable);
    Page<Inquiry> findByCategoryOrderByRegdateDescInquiryIdDesc(String category, Pageable pageable);
    Page<Inquiry> findByStatusOrderByRegdateDescInquiryIdDesc(String status, Pageable pageable);
    Page<Inquiry> findByStatusNotOrderByRegdateDescInquiryIdDesc(String status, Pageable pageable);
    Page<Inquiry> findByCategoryAndStatusOrderByRegdateDescInquiryIdDesc(String category, String status, Pageable pageable);
    Page<Inquiry> findByCategoryAndStatusNotOrderByRegdateDescInquiryIdDesc(String category, String status, Pageable pageable);

    // 사용자용 본인 글 조회
    Page<Inquiry> findByUserOrderByRegdateDescInquiryIdDesc(User user, Pageable pageable);
    Page<Inquiry> findByUserAndCategoryOrderByRegdateDescInquiryIdDesc(User user, String category, Pageable pageable);
    Page<Inquiry> findByUserAndStatusOrderByRegdateDescInquiryIdDesc(User user, String status, Pageable pageable);
    Page<Inquiry> findByUserAndStatusNotOrderByRegdateDescInquiryIdDesc(User user, String status, Pageable pageable);
    Page<Inquiry> findByUserAndCategoryAndStatusOrderByRegdateDescInquiryIdDesc(User user, String category, String status, Pageable pageable);
    Page<Inquiry> findByUserAndCategoryAndStatusNotOrderByRegdateDescInquiryIdDesc(User user, String category, String status, Pageable pageable);
}
