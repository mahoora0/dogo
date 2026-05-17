package com.example.dogo.service.Support;

import com.example.dogo.entity.Support.Notice;
import com.example.dogo.repository.Support.NoticeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public Page<Notice> getNotices(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (category == null || category.isEmpty() || "전체".equals(category)) {
            return noticeRepository.findByDeletedOrderByCreatedAtDesc("N", pageable);
        }
        return noticeRepository.findByCategoryAndDeletedOrderByCreatedAtDesc(category, "N", pageable);
    }

    @Transactional
    public Notice getNoticeDetail(Long id) {
        // DB에 즉시 +1 원자적 반영 및 영속성 컨텍스트 동기화
        noticeRepository.incrementViewCount(id);

        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다. ID: " + id));
        
        if ("Y".equals(notice.getDeleted())) {
            throw new IllegalArgumentException("삭제된 공지사항입니다. ID: " + id);
        }

        return notice;
    }

    @Transactional
    public Notice createNotice(String title, String category, String content, String writer) {
        Notice notice = Notice.builder()
                .title(title)
                .category(category)
                .content(content)
                .writer(writer == null || writer.isEmpty() ? "관리자" : writer)
                .deleted("N")
                .viewCount(0)
                .build();
        return noticeRepository.save(notice);
    }

    @Transactional
    public Notice updateNotice(Long id, String title, String category, String content) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다. ID: " + id));
        notice.setTitle(title);
        notice.setCategory(category);
        notice.setContent(content);
        return notice;
    }

    @Transactional
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다. ID: " + id));
        notice.setDeleted("Y");
    }
}
