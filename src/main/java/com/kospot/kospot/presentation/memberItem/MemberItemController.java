package com.kospot.kospot.presentation.memberItem;

import com.kospot.kospot.application.memberItem.EquipMemberItemUseCase;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.exception.payload.code.SuccessStatus;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "MemberItem Api", description = "멤버 아이템 API")
@RequestMapping("/api/memberItem")
public class MemberItemController {

    private final EquipMemberItemUseCase equipMemberItemUseCase;

    //todo purchase item
    @Operation(summary = "아이템 구매", description = "상점에서 아이템을 구매합니다.")
    @GetMapping("/purchase/{itemId}")
    public ApiResponseDto<?> purchaseItem(Member member, @PathVariable("itemId") Long itemId){
        return null;
    }

    @Operation(summary = "아이템 장착", description = "인벤토리에서 아이템을 장착합니다.")
    @GetMapping("/{memberItemId}")
    public ApiResponseDto<?> equipItem(Member member, @PathVariable("memberItemId") Long memberItemId){
        equipMemberItemUseCase.execute(member, memberItemId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}
