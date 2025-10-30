package com.kospot.presentation.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AdminGameConfigRequest {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotBlank(message = "게임 모드는 필수입니다.")
        private String gameModeKey; // ROADVIEW, PHOTO

        private String playerMatchTypeKey; // SOLO, TEAM (멀티플레이 전용)

        @NotNull(message = "싱글/멀티 모드 구분은 필수입니다.")
        private Boolean isSingleMode; // true: 싱글, false: 멀티
    }
}

