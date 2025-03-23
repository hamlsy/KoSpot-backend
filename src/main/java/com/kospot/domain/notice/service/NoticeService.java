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

    private final NoticeAdaptor noticeAdaptor;
    private final ImageService imageService;
    private final NoticeRepository noticeRepository;

    public void createNotice(NoticeRequest.Create request, List<Image> images) {
        Notice notice = Notice.create(
                request.getTitle(), request.getContent()
        );
        if(images != null) {
            notice.addImages(images);
        }
        noticeRepository.save(notice);
    }

    //todo refactor image, content order
    public void updateNotice(Notice notice, NoticeRequest.Update request) {
        notice.update(request.getTitle(), request.getContent());
    }

    //todo delete
    //todo refactor, 삭제 중 서버 중단?
    public void deleteNotice(Notice notice) {
        noticeRepository.delete(notice);
    }

}
