package com.example.dogo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "FAQ")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FAQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FAQ_ID")
    private Long id;

    @Column(name = "QUESTION", nullable = false, length = 300)
    private String question;

    @Column(name = "ANSWER", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "CATEGORY", length = 50)
    private String category;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive;

    @Column(name = "REGDATE", insertable = false, updatable = false)
    private LocalDateTime regDate;

    @Column(name = "MODDATE", insertable = false, updatable = false)
    private LocalDateTime modDate;

    @PrePersist
    public void prePersist() {
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }
}
