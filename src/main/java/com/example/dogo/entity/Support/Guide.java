package com.example.dogo.entity.Support;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "CONTENT", columnDefinition = "TEXT")
    private String content;

    @Column(name = "CATEGORY", nullable = false, length = 20)
    private String category;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    @Column(name = "IS_DELETED", length = 1)
    private String deleted;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ortOrder ASC")
    @Builder.Default
    private List<GuideImage> guideImages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.deleted == null) {
            this.deleted = "N";
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }

    public void addGuideImage(GuideImage guideImage) {
        if (this.guideImages == null) {
            this.guideImages = new ArrayList<>();
        }
        this.guideImages.add(guideImage);
        guideImage.setGuide(this);
    }
}
