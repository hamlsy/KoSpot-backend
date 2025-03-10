package com.kospot.kospot.presentation.item;

import com.kospot.kospot.application.item.FindAllItemsByTypeUseCase;
import com.kospot.kospot.domain.item.dto.request.ItemRequest;
import com.kospot.kospot.domain.item.dto.response.ItemResponse;
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
    private final ItemService itemService;

    /**
     * TEST
     *
     * @param
     * @return
     */

    @PostMapping("/imageTest")
    public ApiResponseDto<?> imageUploadTest(@ModelAttribute ItemRequest.Create request) {
        itemService.registerItemTest(request);
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

    //todo register item - S3, admin
    @Operation(summary = "아이템 등록", description = "아이템을 등록합니다.")
    @PostMapping("/")
    public ApiResponseDto<?> registerItem(Member member) {
        return null;
    }


    //todo delete item, admin
    @Operation(summary = "아이템 삭제", description = "아이템을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponseDto<?> deleteItem(Member member, @PathVariable("id") Long itemId) {
        return null;
    }

    //todo update item

    @Operation(summary = "아이템 업데이트", description = "아이템을 업데이트 합니다.")
    @PutMapping("/{id}")
    public ApiResponseDto<?> updateItem(Member member, @PathVariable("id") Long itemId) {
        return null;
    }


}
