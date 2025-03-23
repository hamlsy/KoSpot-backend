package com.kospot.application.notice;

import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.service.ImageService;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notice.adaptor.NoticeAdaptor;
import com.kospot.domain.notice.entity.Notice;
import com.kospot.domain.notice.service.NoticeService;
import com.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class DeleteNoticeUseCase {

    private final NoticeAdaptor noticeAdaptor;
    private final NoticeService noticeService;
    private final ImageService imageService;

    public void execute(Member member, Long noticeId) {
        //validate
        member.validateAdmin();
        Notice notice = noticeAdaptor.findByIdFetchImage(noticeId);

        //delete images
        List<Image> images = notice.getImages();
        images.forEach(imageService::deleteImage);

        //delete notice
        noticeService.deleteNotice(notice);
    }

}
