package com.kospot.item.presentation.controller;

import com.kospot.image.application.service.ImageService;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.common.security.aop.CurrentMemberOrNull;
import com.kospot.item.application.usecase.*;
import com.kospot.item.presentation.dto.request.ItemRequest;
import com.kospot.item.presentation.dto.response.ItemResponse;
import com.kospot.item.application.service.ItemService;
import com.kospot.common.exception.payload.code.SuccessStatus;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Item Api", description = "아이템 API")
@RequestMapping("/item")
public class ItemController {

    private final FindAllItemsByTypeUseCase findAllItemsByTypeUseCase;
    private final RegisterItemUseCase registerItemUseCase;
    private final DeleteItemUseCase deleteItemUseCase;
    private final DeleteItemFromShopUseCase deleteItemFromShopUseCase;
    private final RestoreItemToShopUseCase restoreItemToShopUseCase;
    private final UpdateItemInfoUseCase updateItemInfoUseCase;

    private final ItemService itemService;
    private final ImageService imageService;

    /**
     * TEST
     */

//    @PostMapping("/imageTest")
//    public ApiResponseDto<?> imageUploadTest(@ModelAttribute ItemRequest.Create request) {
//        Item item =  itemService.registerItem(request);
//        imageService.uploadItemImage(request.getImage(), item);
//        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
//    }

    /**
     * ----------------------------
     */

    @Operation(summary = "아이템 타입 별 조회", description = "타입 별 아이템들을 조회합니다. 비로그인 사용자는 구매/장착 여부가 모두 false로 반환됩니다.")
    @GetMapping("/{itemTypeKey}")
    public ApiResponseDto<List<ItemResponse>> findItemsByItemType(@CurrentMemberOrNull Long memberId, @PathVariable("itemTypeKey") String itemTypeKey) {
        return ApiResponseDto.onSuccess(findAllItemsByTypeUseCase.execute(memberId, itemTypeKey));
    }

    @Operation(summary = "아이템 등록", description = "아이템을 등록합니다.")
    @PostMapping("/")
    public ApiResponseDto<?> registerItem(@CurrentMember Long memberId,
                                          @ModelAttribute ItemRequest.Create request,
                                          @RequestParam("file") MultipartFile file) {
        registerItemUseCase.execute(memberId, request, file);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "아이템 상점 삭제", description = "아이템을 상점에서 삭제합니다.")
    @PutMapping("/{id}/deleteShop")
    public ApiResponseDto<?> deleteItemFromShop(@CurrentMember Long memberId, @PathVariable("id") Long id) {
        deleteItemFromShopUseCase.execute(memberId, id);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "아이템 상점 재등록", description = "아이템을 상점에 재등록합니다.")
    @PutMapping("/{id}/restoreShop")
    public ApiResponseDto<?> restoreItemToShop(@CurrentMember Long memberId,
                                               @PathVariable("id") Long id) {
        restoreItemToShopUseCase.execute(memberId, id);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    //todo update item
    //todo 사진도 업데이트
    @Operation(summary = "아이템 정보 업데이트", description = "아이템 정보를 업데이트 합니다.")
    @PutMapping("/info")
    public ApiResponseDto<?> updateItemInfo(@CurrentMember Long memberId,
                                            @RequestBody ItemRequest.UpdateInfo request) {
        updateItemInfoUseCase.execute(memberId, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "아이템 삭제", description = "아이템을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponseDto<?> deleteItem(@CurrentMember Long memberId, @PathVariable("id") Long itemId) {
        deleteItemUseCase.execute(memberId, itemId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}
