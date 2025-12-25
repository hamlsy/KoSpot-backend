package com.kospot.presentation.memberitem.dto.response;

import com.kospot.domain.item.vo.ItemType;
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
    private String name;
    private String description;
    private boolean isEquipped;
    private LocalDateTime purchaseTime;

}
