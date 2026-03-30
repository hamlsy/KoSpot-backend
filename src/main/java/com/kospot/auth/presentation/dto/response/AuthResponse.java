package com.kospot.auth.presentation.dto.response;

import com.kospot.common.security.dto.JwtToken;
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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SignUpResult {
        private Long memberId;
        private String accessToken;
        private String refreshToken;

        public static SignUpResult from(Long memberId, JwtToken token) {
            return SignUpResult.builder()
                    .memberId(memberId)
                    .accessToken(token.getAccessToken())
                    .refreshToken(token.getRefreshToken())
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginResult {
        private String accessToken;
        private String refreshToken;

        public static LoginResult from(JwtToken token) {
            return LoginResult.builder()
                    .accessToken(token.getAccessToken())
                    .refreshToken(token.getRefreshToken())
                    .build();
        }
    }
}
