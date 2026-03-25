package com.kospot.mvp.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MvpCommentRequest {

    @Getter
    @NoArgsConstructor
    public static class Create {

        @NotBlank(message = "댓글 내용을 입력해주세요.")
        @Size(max = 300, message = "댓글은 300자 이내여야 합니다.")
        private String content;
    }
}
