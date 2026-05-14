package com.example.dogo.entity.Support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "INQUIRY_FILE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FILE_ID")
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INQUIRY_ID", nullable = false)
    private Inquiry inquiry;

    @Column(name = "ORIGINAL_NAME", nullable = false)
    private String originalName;

    @Column(name = "STORED_NAME", nullable = false)
    private String storedName;

    @Column(name = "FILE_URL", nullable = false)
    private String fileUrl;

    @Column(name = "CONTENT_TYPE")
    private String contentType;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @Column(name = "REGDATE", insertable = false, updatable = false)
    private LocalDateTime regdate;

    public InquiryFile(Inquiry inquiry, String originalName, String storedName, String fileUrl, String contentType, Long fileSize) {
        this.inquiry = inquiry;
        this.originalName = originalName;
        this.storedName = storedName;
        this.fileUrl = fileUrl;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }
}
