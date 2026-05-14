package com.example.dogo.repository;

import com.example.dogo.entity.Inquiry;
import com.example.dogo.entity.InquiryFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryFileRepository extends JpaRepository<InquiryFile, Long> {
    List<InquiryFile> findByInquiryOrderByFileIdAsc(Inquiry inquiry);
}
