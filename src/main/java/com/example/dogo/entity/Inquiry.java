package com.example.dogo.entity;

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

import com.example.dogo.entity.user.User;
import java.time.LocalDateTime;

@Entity
@Table(name = "INQUIRY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INQUIRY_ID")
    private Long inquiryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_NO")
    private User user;

    @Column(name = "CATEGORY", nullable = false)
    private String category;

    @Column(name = "TITLE", nullable = false)
    private String title;

    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "STATUS", nullable = false)
    private String status = "UNREAD";

    @Column(name = "ANSWER")
    private String answer;

    @Column(name = "ANSWERED_AT")
    private LocalDateTime answeredAt;

    @Column(name = "REGDATE", insertable = false, updatable = false)
    private LocalDateTime regdate;

    @Column(name = "MODDATE", insertable = false, updatable = false)
    private LocalDateTime moddate;

    public Inquiry(User user, String category, String title, String content) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public void markChecking() {
        if (!"ANSWERED".equals(status)) {
            this.status = "CHECKING";
        }
    }

    public void answer(String answer) {
        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
        this.status = "ANSWERED";
    }
}
