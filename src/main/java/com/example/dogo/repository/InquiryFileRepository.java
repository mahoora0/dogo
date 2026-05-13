package com.example.dogo.repository;


import com.example.dogo.entity.Inquiry;
import com.example.dogo.entity.InquiryFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryFileRepository extends JpaRepository<InquiryFile, Long> {
    List<InquiryFile> findByInquiryOrderByFileIdAsc(Inquiry inquiry);
}

