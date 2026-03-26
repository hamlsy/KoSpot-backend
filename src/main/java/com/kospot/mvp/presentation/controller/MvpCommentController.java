package com.kospot.mvp.presentation.controller;

import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.mvp.application.usecase.CreateMvpCommentUseCase;
import com.kospot.mvp.application.usecase.DeleteMvpCommentUseCase;
import com.kospot.mvp.application.usecase.GetMvpCommentsUseCase;
import com.kospot.mvp.presentation.dto.request.MvpCommentRequest;
import com.kospot.mvp.presentation.dto.response.MvpCommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "MVP Api", description = "오늘의 MVP API")
@RequestMapping("/mvps")
public class MvpCommentController {

    private final CreateMvpCommentUseCase createMvpCommentUseCase;
    private final DeleteMvpCommentUseCase deleteMvpCommentUseCase;
    private final GetMvpCommentsUseCase getMvpCommentsUseCase;

    @Operation(summary = "MVP 댓글 작성", description = "해당 MVP에 댓글을 작성합니다.")
    @PostMapping("/{dailyMvpId}/comments")
    public ApiResponseDto<MvpCommentResponse.CommentInfo> createComment(
            @CurrentMember Long memberId,
            @PathVariable Long dailyMvpId,
            @Valid @RequestBody MvpCommentRequest.Create request
    ) {
        return ApiResponseDto.onSuccess(
                createMvpCommentUseCase.execute(memberId, dailyMvpId, request.getContent())
        );
    }

    @Operation(summary = "MVP 댓글 목록 조회", description = "해당 MVP의 댓글을 최신순으로 조회합니다. 8개씩 페이징됩니다.")
    @GetMapping("/{dailyMvpId}/comments")
    public ApiResponseDto<MvpCommentResponse.CommentPage> getComments(
            @PathVariable Long dailyMvpId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ApiResponseDto.onSuccess(
                getMvpCommentsUseCase.execute(dailyMvpId, page)
        );
    }

    @Operation(summary = "MVP 댓글 삭제", description = "댓글을 삭제합니다. 작성자 본인 또는 관리자만 삭제할 수 있습니다.")
    @DeleteMapping("/comments/{commentId}")
    public ApiResponseDto<Void> deleteComment(
            @CurrentMember Long memberId,
            @PathVariable Long commentId
    ) {
        deleteMvpCommentUseCase.execute(memberId, commentId);
        return ApiResponseDto.onSuccess(null);
    }
}
