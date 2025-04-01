package com.kospot.application.notice;

import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.service.ImageService;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notice.service.NoticeService;
import com.kospot.global.annotation.usecase.UseCase;
import com.kospot.presentation.notice.dto.request.NoticeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

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
