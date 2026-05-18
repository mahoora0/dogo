package com.example.dogo.entity.Support;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "GUIDE_IMAGE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IMAGE_ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GUIDE_ID", nullable = false)
    private Guide guide;

    @Column(name = "IMAGE_PATH", nullable = false, length = 500)
    private String imagePath;

    @Column(name = "ORT_ORDER")
    private Integer ortOrder;

    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
