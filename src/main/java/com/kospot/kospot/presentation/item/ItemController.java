package com.kospot.kospot.presentation.item;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Item Api", description = "아이템 API")
@RequestMapping("/api/item")
public class ItemController {

    //todo purchaseItem

    //todo getItemList

    //todo register item - S3

    //todo delete item

    //todo update item

    //todo equip item

}
