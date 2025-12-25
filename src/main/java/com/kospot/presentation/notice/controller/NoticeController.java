package com.kospot.presentation.notice.controller;

import com.kospot.application.notice.*;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.notice.dto.request.NoticeRequest;
import com.kospot.presentation.notice.dto.response.NoticeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Notice Api", description = "공지사항 API")
@RequestMapping("/notice")
public class NoticeController {

    private final FindAllNoticePagingUseCase findAllNoticePagingUseCase;
    private final GetDetailNoticeUseCase getDetailNoticeUseCase;
    private final CreateNoticeUseCase createNoticeUseCase;
    private final DeleteNoticeUseCase deleteNoticeUseCase;
    private final UpdateNoticeUseCase updateNoticeUseCase;

    private final UploadNoticeImageUseCase uploadNoticeImageUseCase;

    @Operation(summary = "공지사항 전체 조회", description = "공지사항 전체 리스트를 조회합니다.")
    @GetMapping
    public ApiResponseDto<List<NoticeResponse.Summary>> findAllPaging(
            @RequestParam(value = "page", defaultValue = "0") int page) {
        return ApiResponseDto.onSuccess(findAllNoticePagingUseCase.execute(page));
    }

    @Operation(summary = "공지사항 단일 조회", description = "공지사항 단일 게시물을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponseDto<NoticeResponse.Detail> findDetailNotice(
            @PathVariable("id") Long noticeId) {
        return ApiResponseDto.onSuccess(getDetailNoticeUseCase.execute(noticeId));
    }

    @Operation(summary = "공지사항 생성", description = "공지사항을 생성합니다.")
    @PostMapping
    public ApiResponseDto<?> createNotice(@CurrentMember Member member, @RequestBody NoticeRequest.Create request) {
        createNoticeUseCase.execute(member, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "공지사항 수정", description = "공지사항을 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponseDto<?> updateNotice(@CurrentMember Member member, @PathVariable("id") Long noticeId, @ModelAttribute NoticeRequest.Update request) {
        updateNoticeUseCase.execute(member, noticeId, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    //todo notice delete - admin
    @Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponseDto<?> deleteNotice(Member member, @PathVariable("id") Long noticeId) {
        deleteNoticeUseCase.execute(member, noticeId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "공지사항 이미지 첨부", description = "공지사항 작성 시 이미지를 첨부합니다.")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponseDto<NoticeResponse.NoticeImage> uploadNoticeImage(@CurrentMember Member member, @RequestParam("file") MultipartFile file) {
        NoticeResponse.NoticeImage noticeImage = uploadNoticeImageUseCase.execute(file, member);
        return ApiResponseDto.onSuccess(noticeImage);
    }


}
