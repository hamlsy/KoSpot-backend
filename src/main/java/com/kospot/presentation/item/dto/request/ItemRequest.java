package com.kospot.presentation.item.dto.request;

import com.kospot.domain.image.entity.Image;
import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.vo.ItemType;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

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
