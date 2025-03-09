package com.kospot.kospot.presentation.item;

import com.kospot.kospot.application.item.FindItemsByTypeUseCase;
import com.kospot.kospot.domain.item.dto.response.ItemResponse;
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

    private final FindItemsByTypeUseCase findItemsByTypeUseCase;

    @Operation(summary = "아이템 타입 조회", description = "타입 별 아이템들을 조회합니다.")
    @GetMapping("/{itemTypeKey}")
    public ApiResponseDto<List<ItemResponse.ItemDto>> findItemsByItemType(@PathVariable("itemTypeKey") String itemTypeKey){
        return ApiResponseDto.onSuccess(findItemsByTypeUseCase.execute(itemTypeKey));
    }

    //todo register item - S3, admin
    @Operation(summary = "아이템 등록", description = "아이템을 등록합니다.")
    public ApiResponseDto<?> registerItem(){
        return null;
    }


    //todo delete item, admin
    @Operation(summary = "아이템 삭제", description = "아이템을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponseDto<?> deleteItem(@PathVariable("id") Long itemId){
        return null;
    }

    //todo update item

    @Operation(summary = "아이템 업데이트", description = "아이템을 업데이트 합니다.")
    @PutMapping("/{id}")
    public ApiResponseDto<?> updateItem(@PathVariable("id") Long itemId){
        return null;
    }


}
