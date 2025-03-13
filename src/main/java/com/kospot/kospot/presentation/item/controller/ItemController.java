package com.kospot.kospot.presentation.item.controller;

import com.kospot.kospot.application.item.*;
import com.kospot.kospot.domain.image.service.ImageService;
import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.presentation.item.dto.request.ItemRequest;
import com.kospot.kospot.presentation.item.dto.response.ItemResponse;
import com.kospot.kospot.domain.item.service.ItemService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.exception.payload.code.SuccessStatus;
import com.kospot.kospot.exception.payload.dto.ApiResponseDto;
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
@Tag(name = "Item Api", description = "아이템 API")
@RequestMapping("/api/item")
public class ItemController {

    private final FindAllItemsByTypeUseCase findAllItemsByTypeUseCase;
    private final RegisterItemUseCase registerItemUseCase;
    private final DeleteAllItemUseCase deleteAllItemUseCase;
    private final DeleteItemFromShopUseCase deleteItemFromShopUseCase;
    private final RestoreItemToShopUseCase restoreItemToShopUseCase;

    private final ItemService itemService;
    private final ImageService imageService;

    /**
     * TEST
     */

    @PostMapping("/imageTest")
    public ApiResponseDto<?> imageUploadTest(@ModelAttribute ItemRequest.Create request) {
        Item item =  itemService.registerItemTest(request);
        imageService.uploadItemImage(request.getImage(), item);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    /**
     * ----------------------------
     */

    @Operation(summary = "아이템 타입 조회", description = "타입 별 아이템들을 조회합니다.")
    @GetMapping("/{itemTypeKey}")
    public ApiResponseDto<List<ItemResponse.ItemDto>> findItemsByItemType(@PathVariable("itemTypeKey") String itemTypeKey) {
        return ApiResponseDto.onSuccess(findAllItemsByTypeUseCase.execute(itemTypeKey));
    }

    @Operation(summary = "아이템 등록", description = "아이템을 등록합니다.")
    @PostMapping("/")
    public ApiResponseDto<?> registerItem(Member member, @ModelAttribute ItemRequest.Create request) {
        registerItemUseCase.execute(member, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "아이템 상점 삭제", description = "아이템을 상점에서 삭제합니다.")
    @PutMapping("/{id}/deleteShop")
    public ApiResponseDto<?> deleteItemFromShop(Member member, @PathVariable("id") Long id) {
        deleteItemFromShopUseCase.execute(member, id);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "아이템 상점 재등록", description = "아이템을 상점에 재등록합니다.")
    @PutMapping("/{id}/restoreShop")
    public ApiResponseDto<?> restoreItemToShop(Member member, @PathVariable("id") Long id) {
        restoreItemToShopUseCase.execute(member, id);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    //todo update item
    @Operation(summary = "아이템 정보 업데이트", description = "아이템 정보를 업데이트 합니다.")
    @PutMapping("/{id}/info")
    public ApiResponseDto<?> updateItemInfo(Member member, @PathVariable("id") Long itemId) {
        return null;
    }

    @Operation(summary = "아이템 사진 업데이트", description = "아이템 사진을 업데이트 합니다.")
    @PutMapping("/{id}/image")
    public ApiResponseDto<?> updateItemImage(Member member, @PathVariable("id") Long itemId) {

        return null;
    }

    @Operation(summary = "아이템 삭제", description = "아이템을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponseDto<?> deleteItem(Member member, @PathVariable("id") Long itemId) {
        deleteAllItemUseCase.execute(member, itemId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

}
