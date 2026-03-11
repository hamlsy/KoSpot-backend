package com.kospot.member.presentation.controller;

import com.kospot.common.annotation.adsense.BotSuccess;
import com.kospot.common.exception.payload.code.SuccessStatus;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.member.application.usecase.*;
import com.kospot.member.presentation.response.MemberProfileResponse;
import com.kospot.member.presentation.response.MemberShopInfoResponse;
import com.kospot.member.presentation.response.PlayerSummaryResponse;
import com.kospot.member.presentation.response.SearchMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Member Api", description = "회원 API")
@RequestMapping("/member")
public class MemberController {

    private final GetMemberProfileUseCase getMemberProfileUseCase;
    private final GetMemberShopInfoUseCase getMemberShopInfoUseCase;
    private final GetPlayerSummaryUseCase getPlayerSummaryUseCase;
    private final SetNicknameUseCase setNicknameUseCase;
    private final UpdateNicknameUseCase updateNicknameUseCase;
    private final SearchMembersByNicknameUseCase searchMembersByNickNameUseCase;

    @Operation(summary = "내 정보 조회", description = "회원의 프로필과 게임 통계, 랭킹, 아이템 정보를 조회합니다.")
    @GetMapping("/profile")
    @BotSuccess
    public ApiResponseDto<MemberProfileResponse> getMemberProfile(@CurrentMember Long memberId) {
        MemberProfileResponse response = getMemberProfileUseCase.execute(memberId);
        return ApiResponseDto.onSuccess(response);
    }

    @Operation(summary = "상점 내 정보 조회", description = "상점 페이지에서 필요한 내 포인트, 장착 아이템, 보유 아이템 정보를 조회합니다.")
    @GetMapping("/shop-info")
    public ApiResponseDto<MemberShopInfoResponse> getMemberShopInfo(@CurrentMember Long memberId) {
        MemberShopInfoResponse response = getMemberShopInfoUseCase.execute(memberId);
        return ApiResponseDto.onSuccess(response);
    }

    @Operation(summary = "플레이어 간략 정보 조회", description = "특정 플레이어의 간략한 정보를 조회합니다. (닉네임, 연속 플레이, 마커 이미지, 랭크 정보, 멀티플레이 통계)")
    @GetMapping("/{memberId}/summary")
    public ApiResponseDto<PlayerSummaryResponse> getPlayerSummary(@PathVariable("memberId") Long memberId) {
        PlayerSummaryResponse response = getPlayerSummaryUseCase.execute(memberId);
        return ApiResponseDto.onSuccess(response);
    }

    @Operation(summary = "테스트용 멤버 조회", description = "테스트용 멤버 조회")
    @GetMapping("/me")
    public ApiResponseDto<String> testCurrentMember(@CurrentMember Long memberId) {
        return ApiResponseDto.onSuccess(String.valueOf(memberId));
    }

    @Operation(summary = "닉네임 설정", description = "회원의 닉네임을 설정합니다.")
    @PostMapping("/set-nickname")
    public ApiResponseDto<?> setNickname(@CurrentMember Long memberId, @RequestParam("nickname") String nickname) {
        setNicknameUseCase.execute(memberId, nickname);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "닉네임 업데이트", description = "회원의 닉네임을 업데이트합니다.")
    @PostMapping("/update-nickname")
    public ApiResponseDto<?> updateNickname(@CurrentMember Long memberId, @RequestParam("nickname") String nickname) {
        updateNicknameUseCase.execute(memberId, nickname);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "친구 추가할 멤버 닉네임으로 조회", description = "친구 추가 시, 닉네임으로 멤버를 검색하여 친구 요청을 보낼 수 있도록 합니다.")
    @GetMapping("/search")
    public ApiResponseDto<List<SearchMemberResponse>> searchMembersByNickname(
            @CurrentMember Long memberId,
            @RequestParam("nickname") String nickname
    ) {
        return ApiResponseDto.onSuccess(searchMembersByNickNameUseCase.execute(memberId, nickname));
    }

}
