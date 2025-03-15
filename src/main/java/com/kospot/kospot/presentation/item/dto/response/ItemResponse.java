package com.kospot.kospot.presentation.item.dto.response;

import com.kospot.kospot.domain.item.entity.Item;
import lombok.Builder;
import lombok.Getter;

public class ItemResponse {

    @Getter
    @Builder
    public static class ItemDto {

        private Long itemId;
        private String name;
        private String description;
        private int price;
        private int quantity;
        private String imageUrl;
        private boolean isOwned;

        public static ItemDto from(Item item){
            return builder()
                    .itemId(item.getId())
                    .name(item.getName())
                    .imageUrl(item.getImage().getImageUrl())
                    .description(item.getDescription())
                    .build();
        }
    }


}
