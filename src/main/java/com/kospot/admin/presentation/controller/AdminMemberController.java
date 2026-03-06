package com.kospot.admin.presentation.controller;

import com.kospot.admin.application.usecase.member.FindAllMembersUseCase;
import com.kospot.admin.application.usecase.member.FindMemberDetailUseCase;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.admin.presentation.dto.response.AdminMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Admin Member Api", description = "관리자 - 회원 관리 API")
@RequestMapping("/admin/members")
public class AdminMemberController {

    private final FindAllMembersUseCase findAllMembersUseCase;
    private final FindMemberDetailUseCase findMemberDetailUseCase;

    @Operation(summary = "회원 목록 조회", description = "관리자가 회원 목록을 페이징 조회합니다.")
    @GetMapping
    public ApiResponseDto<Page<AdminMemberResponse.MemberInfo>> findAllMembers(
            @CurrentMember Long adminId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "role", required = false) String role
    ) {
        Page<AdminMemberResponse.MemberInfo> members = findAllMembersUseCase.execute(adminId, pageable, role);
        return ApiResponseDto.onSuccess(members);
    }

    @Operation(summary = "회원 상세 조회", description = "관리자가 특정 회원의 상세 정보를 조회합니다.")
    @GetMapping("/{memberId}")
    public ApiResponseDto<AdminMemberResponse.MemberDetail> findMemberDetail(
            @CurrentMember Long adminId,
            @PathVariable("memberId") Long memberId
    ) {
        AdminMemberResponse.MemberDetail memberDetail = findMemberDetailUseCase.execute(adminId, memberId);
        return ApiResponseDto.onSuccess(memberDetail);
    }
}

