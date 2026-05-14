package com.example.dogo.repository.Support;

import com.example.dogo.entity.Support.Inquiry;
import com.example.dogo.entity.Support.InquiryFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryFileRepository extends JpaRepository<InquiryFile, Long> {
    List<InquiryFile> findByInquiryOrderByFileIdAsc(Inquiry inquiry);
}
