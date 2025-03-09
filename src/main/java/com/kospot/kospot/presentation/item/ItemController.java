package com.kospot.kospot.presentation.item;

import com.kospot.kospot.application.item.FindItemsByTypeUseCase;
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
@Tag(name = "Item Api", description = "아이템 API")
@RequestMapping("/api/item")
public class ItemController {

    private final FindItemsByTypeUseCase findItemsByTypeUseCase;

    @Operation(summary = "아이템 타입 조회", description = "타입 별 아이템들을 조회합니다.")
    @GetMapping("/{itemTypeKey}")
    public ApiResponseDto<?> findItemsByItemType(@PathVariable("itemTypeKey") String itemTypeKey){
        return ApiResponseDto.onSuccess(findItemsByTypeUseCase.execute(itemTypeKey));
    }

    //todo register item - S3

    //todo delete item

    //todo update item


}
