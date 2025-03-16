package com.kospot.kospot.presentation.memberItem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberItemResponse {

    private Long memberItemId;
    private String name;
    private String description;
    private boolean isEquipped;
    private LocalDateTime purchaseTime;

}
