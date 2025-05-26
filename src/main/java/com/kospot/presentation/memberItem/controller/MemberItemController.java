package com.kospot.presentation.memberItem.controller;

import com.kospot.application.memberItem.EquipMemberItemUseCase;
import com.kospot.application.memberItem.FindAllMemberItemsByItemTypeUseCase;
import com.kospot.application.memberItem.PurchaseItemUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.global.exception.payload.code.SuccessStatus;
import com.kospot.global.exception.payload.dto.ApiResponseDto;
import com.kospot.presentation.memberItem.dto.response.MemberItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "MemberItem Api", description = "멤버 아이템 API")
@RequestMapping("/memberItem")
public class MemberItemController {

    private final EquipMemberItemUseCase equipMemberItemUseCase;
    private final FindAllMemberItemsByItemTypeUseCase findAllMemberItemsByItemTypeUseCase;
    private final PurchaseItemUseCase purchaseItemUseCase;

    //todo refactoring
    @Operation(summary = "아이템 구매", description = "상점에서 아이템을 구매합니다.")
    @GetMapping("/{itemId}/purchase")
    public ApiResponseDto<?> purchaseItem(Member member, @PathVariable("itemId") Long itemId) {
        purchaseItemUseCase.execute(member, itemId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "아이템 장착", description = "인벤토리에서 아이템을 장착합니다.")
    @GetMapping("/{memberItemId}")
    public ApiResponseDto<?> equipItem(Member member, @PathVariable("memberItemId") Long memberItemId) {
        equipMemberItemUseCase.execute(member, memberItemId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "내 아이템 조회", description = "내 인벤토리에서 타입 별 아이템들을 조회합니다.")
    @GetMapping("/{itemType}")
    public ApiResponseDto<List<MemberItemResponse>> findAllMemberItemByItemType(
            Member member, @PathVariable("itemType") String itemType) {
        return ApiResponseDto.onSuccess(findAllMemberItemsByItemTypeUseCase.execute(member, itemType));
    }

}
