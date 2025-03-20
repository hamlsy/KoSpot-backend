package com.kospot.kospot.application.notice;

import com.kospot.kospot.domain.image.entity.Image;
import com.kospot.kospot.domain.image.service.ImageService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.notice.service.NoticeService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.notice.dto.request.NoticeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class CreateNoticeUseCase {

    private final ImageService imageService;
    private final NoticeService noticeService;

    public void execute(Member member, NoticeRequest.Create request) {
        member.validateAdmin();
        List<Image> images = imageService.uploadNoticeImages(request.getImages());
        noticeService.createNotice(request, images);
    }

}
