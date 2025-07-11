package com.kospot.presentation.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthRequest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReIssue {
        private String refreshToken;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Logout {
        private String refreshToken;
    }

}
