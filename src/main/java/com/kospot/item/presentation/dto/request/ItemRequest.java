package com.kospot.item.presentation.dto.request;

import com.kospot.image.domain.entity.Image;
import com.kospot.item.domain.entity.Item;
import com.kospot.item.domain.vo.ItemType;
import lombok.*;

public class ItemRequest {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        private String name;
        private String description;
        private int price;
        private String itemTypeKey;
        private int quantity;

        public Item toEntity(Image image){
            return Item.create(name, description,
                    ItemType.fromKey(itemTypeKey), price, quantity, image);
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
