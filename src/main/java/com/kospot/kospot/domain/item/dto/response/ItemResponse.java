package com.kospot.kospot.domain.item.dto.response;

import com.kospot.kospot.domain.item.entity.Item;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class ItemResponse {

    @Getter
    @Builder
    public static class ItemDto {

        private Long itemId;
        private String name;
        private String imageUrl;
        private String description;
        private int price;

        public static ItemDto from(Item item){
            return builder()
                    .itemId(item.getId())
                    .name(item.getName())
                    .imageUrl(item.getImageUrl())
                    .description(item.getDescription())
                    .build();
        }
    }


}
