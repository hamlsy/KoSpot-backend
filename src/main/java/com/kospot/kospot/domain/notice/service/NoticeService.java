package com.kospot.kospot.domain.notice.service;

import com.kospot.kospot.domain.image.entity.Image;
import com.kospot.kospot.domain.image.service.ImageService;
import com.kospot.kospot.domain.notice.entity.Notice;
import com.kospot.kospot.domain.notice.repository.NoticeRepository;
import com.kospot.kospot.presentation.notice.dto.request.NoticeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final ImageService imageService;
    private final NoticeRepository noticeRepository;

    public void createNotice(NoticeRequest.Create request, List<Image> images) {
        Notice notice = Notice.create(
                request.getTitle(), request.getContent(), images
        );
        noticeRepository.save(notice);
    }

    //todo update
    public void updateNotice() {

    }

    //todo delete
    public void deleteNotice() {

    }

}
