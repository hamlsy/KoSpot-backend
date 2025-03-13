package com.kospot.kospot.presentation.image.controller;

import com.kospot.kospot.application.image.UpdateImageUseCase;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.exception.payload.code.SuccessStatus;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import com.kospot.kospot.presentation.image.dto.request.ImageRequest;
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
@RequestMapping("/api/image")
public class ImageController {

    private final UpdateImageUseCase updateImageUseCase;

    @Operation(summary = "이미지 수정", description = "이미지를 수정합니다.")
    @PutMapping("/")
    public ApiResponseDto<?> updateImage(Member member, ImageRequest.Update request){
        updateImageUseCase.execute(member, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}
