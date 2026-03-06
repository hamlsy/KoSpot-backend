package com.kospot.notice.application.service;

import com.kospot.notice.domain.entity.Notice;
import com.kospot.notice.infrastructure.persistence.NoticeRepository;
import com.kospot.presentation.notice.dto.request.NoticeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeContentRenderer noticeContentRenderer;

    public Notice createNotice(NoticeRequest.Create request) {
        String safeHtml = noticeContentRenderer.renderToSafeHtml(request.getContentMd());
        Notice notice = Notice.create(
                request.getTitle(), request.getContentMd(), safeHtml
        );
        return noticeRepository.save(notice);
    }

    public void updateNotice(Notice notice, NoticeRequest.Update request) {
        String safeHtml = noticeContentRenderer.renderToSafeHtml(request.getContentMd());
        notice.update(request.getTitle(), request.getContentMd(), safeHtml);
    }

    //todo delete
    //todo refactor, 삭제 중 서버 중단?
    public void deleteNotice(Notice notice) {
        noticeRepository.delete(notice);
    }

}
