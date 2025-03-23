package com.kospot.application.image;

import com.kospot.domain.image.service.ImageService;
import com.kospot.domain.member.entity.Member;
import com.kospot.global.annotation.usecase.UseCase;
import com.kospot.presentation.image.dto.request.ImageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class UpdateImageUseCase {

    private final ImageService imageService;

    public void execute(Member member, ImageRequest.Update request) {
        member.validateAdmin();
        imageService.updateImage(request);
    }

}
