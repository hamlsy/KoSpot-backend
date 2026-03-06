package com.kospot.admin.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AdminNotificationRequest {

    public enum TargetType {
        ALL,
        MEMBERS
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SendMessage {

        @NotNull
        private TargetType targetType;

        // targetType=MEMBERS일 때만 사용
        private List<Long> memberIds;

        @NotBlank
        @Size(max = 200)
        private String title;

        @NotBlank
        @Size(max = 2000)
        private String content;
    }
}
