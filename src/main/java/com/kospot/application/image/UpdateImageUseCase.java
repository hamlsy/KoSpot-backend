package com.kospot.application.image;

import com.kospot.domain.image.service.ImageService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.image.dto.request.ImageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class UpdateImageUseCase {

    private final MemberAdaptor memberAdaptor;
    private final ImageService imageService;

    public void execute(Long memberId, ImageRequest.Update request) {
        Member member = memberAdaptor.queryById(memberId);
        member.validateAdmin();
        imageService.updateImage(request);
    }

}
