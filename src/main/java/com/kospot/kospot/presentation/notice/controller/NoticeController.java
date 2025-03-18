package com.kospot.kospot.presentation.notice.controller;

import com.kospot.kospot.application.notice.CreateNoticeUseCase;
import com.kospot.kospot.application.notice.DeleteNoticeUseCase;
import com.kospot.kospot.application.notice.FindAllNoticePagingUseCase;
import com.kospot.kospot.application.notice.FindDetailNoticeUseCase;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.exception.payload.code.SuccessStatus;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import com.kospot.kospot.presentation.notice.dto.request.NoticeRequest;
import com.kospot.kospot.presentation.notice.dto.response.NoticeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Notice Api", description = "공지사항 API")
@RequestMapping("/api/notice")
public class NoticeController {

    private final FindAllNoticePagingUseCase findAllNoticePagingUseCase;
    private final FindDetailNoticeUseCase findDetailNoticeUseCase;
    private final CreateNoticeUseCase createNoticeUseCase;
    private final DeleteNoticeUseCase deleteNoticeUseCase;

    @Operation(summary = "공지사항 전체 조회", description = "공지사항 전체 리스트를 조회합니다.")
    @GetMapping("/")
    public ApiResponseDto<List<NoticeResponse.Summary>> findAllPaging(
            @RequestParam(value = "page", defaultValue = "0") int page) {
        return ApiResponseDto.onSuccess(findAllNoticePagingUseCase.execute(page));
    }

    @Operation(summary = "공지사항 단일 조회", description = "공지사항 단일 게시물을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponseDto<NoticeResponse.Detail> findDetailNotice(
            @PathVariable("id") Long noticeId) {
        return ApiResponseDto.onSuccess(findDetailNoticeUseCase.execute(noticeId));
    }

    //todo html notice create(insert image) - admin
    @Operation(summary = "공지사항 생성", description = "공지사항을 생성합니다.")
    @PostMapping("/")
    public ApiResponseDto<?> createNotice(Member member, @ModelAttribute NoticeRequest.Create request) {
        createNoticeUseCase.execute(member, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    //todo html notice update - admin
    @Operation(summary = "공지사항 수정", description = "공지사항을 수정합니다.")
    @PutMapping("/")
    public ApiResponseDto<?> updateNotice(Member member) {
        return null;
    }

    //todo notice delete - admin
    @Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponseDto<?> deleteNotice(Member member, @PathVariable("id") Long noticeId) {
        deleteNoticeUseCase.execute(member, noticeId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}
