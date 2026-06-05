package com.example.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Value;

public class AuthDto {

    @Data
    public static class RegisterRequest {
        @Email(message = "Неверный формат email")
        @NotBlank(message = "Email обязателен")
        private String email;

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, message = "Пароль не менее 8 символов")
        private String password;

        @NotBlank(message = "Имя обязательно")
        @Size(max = 100)
        private String name;
    }

    @Data
    public static class LoginRequest {
        @Email @NotBlank private String email;
        @NotBlank        private String password;
        private String totpCode;
    }

    @Data
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private boolean twoFactorRequired;

        public AuthResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public static AuthResponse twoFactorRequired() {
            AuthResponse r = new AuthResponse(null, null);
            r.twoFactorRequired = true;
            return r;
        }
    }

    @Value
    public static class MessageResponse { String message; }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank private String refreshToken;
    }

    @Value
    public static class TwoFactorSetupResponse {
        String secret;
        String qrCodeUrl;
    }

    @Data
    public static class TwoFactorVerifyRequest {
        @NotBlank private String code;
    }
}
