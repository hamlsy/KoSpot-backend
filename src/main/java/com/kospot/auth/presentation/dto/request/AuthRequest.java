package com.kospot.auth.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SignUp {
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 8)
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LocalLogin {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PasswordResetRequest {
        @NotBlank @Email
        private String email;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfirmPasswordReset {
        @NotBlank
        private String token;
        @NotBlank @Size(min = 8)
        private String newPassword;
    }

}
