package com.kospot.notice.application.usecase;

import com.kospot.image.domain.entity.Image;
import com.kospot.image.application.service.ImageService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.notice.presentation.dto.response.NoticeResponse;
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
