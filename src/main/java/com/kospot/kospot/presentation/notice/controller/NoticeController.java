package com.kospot.kospot.presentation.notice.controller;

import com.kospot.kospot.application.notice.FindAllNoticePagingUseCase;
import com.kospot.kospot.application.notice.FindDetailNoticeUseCase;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
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

    @Operation(summary = "공지사항 전체 조회", description = "공지사항 전체 리스트를 조회합니다.")
    @GetMapping("/")
    public ApiResponseDto<List<NoticeResponse.Summary>> findAllPaging(
            @RequestParam(value = "page", defaultValue = "0") int page) {
        return ApiResponseDto.onSuccess(findAllNoticePagingUseCase.execute(page));
    }

    @Operation(summary = "공지사항 단일 조회", description = "공지사항 단일 게시물을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponseDto<?> findDetailNotice(
            @PathVariable("id") Long noticeId) {
        return ApiResponseDto.onSuccess(findDetailNoticeUseCase.execute(noticeId));
    }

    //todo html notice create(insert image) - admin

    //todo html notice update - admin

    //todo notice delete - admin


}
