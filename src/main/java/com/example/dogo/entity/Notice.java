package com.example.dogo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NOTICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTICE_ID")
    private Long id;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "CONTENT", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "CATEGORY", nullable = false, length = 50)
    private String category;

    @Column(name = "IS_DELETED", length = 1)
    private String deleted;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "WRITER", nullable = false, length = 50)
    private String writer;

    @Column(name = "VIEW_COUNT")
    private Integer viewCount;

    @PrePersist
    public void prePersist() {
        if (this.deleted == null) {
            this.deleted = "N";
        }
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
    }

    public void incrementViewCount() {
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        this.viewCount++;
    }
}
