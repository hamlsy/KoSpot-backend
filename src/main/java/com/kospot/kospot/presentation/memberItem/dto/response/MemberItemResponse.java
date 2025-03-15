package com.kospot.kospot.presentation.memberItem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class MemberItemResponse {

    @Getter
    @Builder
    public static class MemberItemDto {

        private Long memberItemId;
        private String name;
        private String description;
        private boolean isEquipped;
        private LocalDateTime purchaseTime;

    }

}
