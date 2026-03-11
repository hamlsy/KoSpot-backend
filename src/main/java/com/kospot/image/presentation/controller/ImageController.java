package com.kospot.image.presentation.controller;

import com.kospot.image.application.usecase.UpdateImageUseCase;
import com.kospot.image.application.service.ImageService;
import com.kospot.common.exception.payload.code.SuccessStatus;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.image.presentation.request.ImageRequest;
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
    public ApiResponseDto<?> updateImage(@CurrentMember Long memberId, ImageRequest.Update request){
        updateImageUseCase.execute(memberId, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}
