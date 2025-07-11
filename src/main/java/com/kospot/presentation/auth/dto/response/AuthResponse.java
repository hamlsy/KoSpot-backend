package com.kospot.presentation.auth.dto.response;

import com.kospot.infrastructure.security.dto.JwtToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthResponse {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TempLogin {
        private Long memberId;
        private String accessToken;
        private String refreshToken;

        public static TempLogin from(JwtToken jwtToken, Long memberId) {
            return TempLogin.builder()
                    .memberId(memberId) // Temp login does not have a member ID
                    .accessToken(jwtToken.getAccessToken())
                    .refreshToken(jwtToken.getRefreshToken())
                    .build();
        }

    }
}
