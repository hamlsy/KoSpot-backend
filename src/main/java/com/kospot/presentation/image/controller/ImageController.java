package com.kospot.presentation.image.controller;

import com.kospot.application.image.UpdateImageUseCase;
import com.kospot.domain.image.service.ImageService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.image.dto.request.ImageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Image Api", description = "이미지 API")
@RequestMapping("/image")
public class ImageController {

    private final UpdateImageUseCase updateImageUseCase;

    private final ImageService imageService;

    /**
     * Test
     */

    @PutMapping("/test")
    public ApiResponseDto<?> testUpdateImage(ImageRequest.Update request){
        imageService.updateImage(request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    /**
     *  -----------------------------------
     */

    @Operation(summary = "이미지 수정", description = "이미지를 수정합니다.")
    @PutMapping("/")
    public ApiResponseDto<?> updateImage(@CurrentMember Member member, ImageRequest.Update request){
        updateImageUseCase.execute(member, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}
