package com.kospot.item.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kospot.item.domain.entity.Item;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ItemResponse {

    private Long itemId;
    private String name;
    private String description;
    private int price;
    private int stock;
    private String imageUrl;
    @JsonProperty("isOwned")
    private boolean isOwned;
    private Long ownedMemberItemId;
    @JsonProperty("isEquipped")
    private boolean isEquipped;

    public static ItemResponse from(Item item) {
        return ItemResponse.builder()
                .itemId(item.getId())
                .name(item.getName())
                .imageUrl(item.getImage().getImageUrl())
                .description(item.getDescription())
                .build();
    }
}

