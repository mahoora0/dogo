package com.example.dogo.entity.Support;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "GUIDE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GUIDE_ID")
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

    @PrePersist
    public void prePersist() {
        if (this.deleted == null) {
            this.deleted = "N";
        }
    }
}
