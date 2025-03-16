package com.kospot.kospot.presentation.item.dto.response;

import com.kospot.kospot.domain.item.entity.Item;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemResponse {

    private Long itemId;
    private String name;
    private String description;
    private int price;
    private int stock;
    private String imageUrl;
    private boolean isOwned;

    public static ItemResponse from(Item item) {
        return ItemResponse.builder()
                .itemId(item.getId())
                .name(item.getName())
                .imageUrl(item.getImage().getImageUrl())
                .description(item.getDescription())
                .build();
    }
}

