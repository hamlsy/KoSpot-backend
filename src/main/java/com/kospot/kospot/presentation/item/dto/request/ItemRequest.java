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

        public Item toEntity(){
            return Item.create(name, description,
                    ItemType.fromKey(itemTypeKey), price);
        }

    }

}
