package com.kospot.memberitem.presentation.controller;

import com.kospot.memberitem.application.usecase.EquipMemberItemUseCase;
import com.kospot.memberitem.application.usecase.FindAllMemberItemsByItemTypeUseCase;
import com.kospot.memberitem.application.usecase.FindAllMemberItemsUseCase;
import com.kospot.memberitem.application.usecase.PurchaseItemUseCase;
import com.kospot.common.exception.payload.code.SuccessStatus;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.memberitem.presentation.response.MemberItemResponse;
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
@Tag(name = "MemberItem Api", description = "멤버 아이템 API")
@RequestMapping("/memberItem")
public class MemberItemController {

    private final EquipMemberItemUseCase equipMemberItemUseCase;
    private final FindAllMemberItemsByItemTypeUseCase findAllMemberItemsByItemTypeUseCase;
    private final FindAllMemberItemsUseCase findAllMemberItemsUseCase;
    private final PurchaseItemUseCase purchaseItemUseCase;

    //todo refactoring
    @Operation(summary = "아이템 구매", description = "상점에서 아이템을 구매합니다.")
    @GetMapping("/{itemId}/purchase")
    public ApiResponseDto<?> purchaseItem(@CurrentMember Long memberId, @PathVariable("itemId") Long itemId) {
        purchaseItemUseCase.execute(memberId, itemId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "아이템 장착", description = "인벤토리에서 아이템을 장착합니다.")
    @PutMapping("/{memberItemId}")
    public ApiResponseDto<?> equipItem(@CurrentMember Long memberId, @PathVariable("memberItemId") Long memberItemId) {
        equipMemberItemUseCase.execute(memberId, memberItemId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "내 아이템 조회", description = "내 인벤토리에서 타입 별 아이템들을 조회합니다.")
    @GetMapping("/{itemType}")
    public ApiResponseDto<List<MemberItemResponse>> findAllMemberItemByItemType(
            @CurrentMember Long memberId, @PathVariable("itemType") String itemType) {
        return ApiResponseDto.onSuccess(findAllMemberItemsByItemTypeUseCase.execute(memberId, itemType));
    }

    @Operation(summary = "내 아이템 전체 조회", description = "내 인벤토리에서 아이템들을 조회합니다.")
    @GetMapping("/inventory")
    public ApiResponseDto<List<MemberItemResponse>> findAllMemberItems(@CurrentMember Long memberId) {
        return ApiResponseDto.onSuccess(findAllMemberItemsUseCase.execute(memberId));
    }

}
