package com.kospot.presentation.member.controller;

import com.kospot.application.member.GetMemberProfileUseCase;
import com.kospot.application.member.SetNicknameUseCase;
import com.kospot.application.member.UpdateNicknameUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.member.dto.response.MemberProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Member Api", description = "회원 API")
@RequestMapping("/member")
public class MemberController {

    private final GetMemberProfileUseCase getMemberProfileUseCase;
    private final SetNicknameUseCase setNicknameUseCase;
    private final UpdateNicknameUseCase updateNicknameUseCase;

    @Operation(summary = "내 정보 조회", description = "회원의 프로필과 게임 통계, 랭킹, 아이템 정보를 조회합니다.")
    @GetMapping("/profile")
    public ApiResponseDto<MemberProfileResponse> getMemberProfile(@CurrentMember Member member) {
        MemberProfileResponse response = getMemberProfileUseCase.execute(member);
        return ApiResponseDto.onSuccess(response);
    }

    @Operation(summary = "테스트용 멤버 조회", description = "테스트용 멤버 조회")
    @GetMapping("/me")
    public ApiResponseDto<String> testCurrentMember(@CurrentMember Member member) {
        return ApiResponseDto.onSuccess(member.getUsername());
    }

    @Operation(summary = "닉네임 설정", description = "회원의 닉네임을 설정합니다.")
    @PostMapping("/set-nickname")
    public ApiResponseDto<?> setNickname(@CurrentMember Member member, @RequestParam("nickname") String nickname) {
        setNicknameUseCase.execute(member, nickname);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "닉네임 업데이트", description = "회원의 닉네임을 업데이트합니다.")
    @PostMapping("/update-nickname")
    public ApiResponseDto<?> updateNickname(@CurrentMember Member member, @RequestParam("nickname") String nickname) {
        updateNicknameUseCase.execute(member, nickname);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}
