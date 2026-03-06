package com.kospot.application.notice;

import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.service.ImageService;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.notice.dto.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class UploadNoticeImageUseCase {

    private final MemberAdaptor memberAdaptor;
    private final ImageService imageService;

    public NoticeResponse.NoticeImage execute(MultipartFile file, Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        member.validateAdmin();
        Image image = imageService.uploadNoticeImage(file);
        return NoticeResponse.NoticeImage.from(image);
    }

}
