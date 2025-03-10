package com.kospot.kospot.domain.item.dto.request;

import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

public class ItemRequest {

    @Getter
    @Builder
    public static class Create {

        private String name;
        private MultipartFile image;
        private String description;
        private int price;
        private String itemTypeKey;

        public Item toEntity(String imageUrl){
            return Item.create(name, description,
                    ItemType.fromKey(itemTypeKey), price,
                    imageUrl
            );
        }

    }

}
