package com.kospot.domain.notice.service;

import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.service.ImageService;
import com.kospot.domain.notice.adaptor.NoticeAdaptor;
import com.kospot.domain.notice.entity.Notice;
import com.kospot.domain.notice.repository.NoticeRepository;
import com.kospot.presentation.notice.dto.request.NoticeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
