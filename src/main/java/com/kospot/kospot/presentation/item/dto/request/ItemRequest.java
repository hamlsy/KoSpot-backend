package com.kospot.kospot.presentation.item.dto.request;

import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

public class ItemRequest {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        private String name;
        private MultipartFile image;
        private String description;
        private int price;
        private String itemTypeKey;
        private int quantity;

        public Item toEntity(){
            return Item.create(name, description,
                    ItemType.fromKey(itemTypeKey), price, quantity);
        }

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateInfo {

        private Long itemId;
        private String name;
        private String description;
        private int price;
        private String itemTypeKey;
        private int quantity;

    }

}
