package com.kospot.memberitem.presentation.response;

import com.kospot.item.domain.vo.ItemType;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MemberItemResponse {

    private Long memberItemId;
    private ItemType itemType;
    private String itemImageUrl;
    private String name;
    private String description;
    private Boolean isEquipped;
    private LocalDateTime purchaseTime;

}
