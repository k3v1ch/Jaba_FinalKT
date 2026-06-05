package com.example.auth.dto;

import com.example.auth.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class UserProfileDto {

    @Data
    @AllArgsConstructor
    public static class ProfileResponse {
        private Long id;
        private String email;
        private String name;
        private String bio;
        private String role;
        private LocalDateTime createdAt;
        private List<ImageInfo> images;

        public static ProfileResponse from(User user, List<ImageInfo> images) {
            return new ProfileResponse(
                    user.getId(), user.getEmail(), user.getName(),
                    user.getBio(), user.getRole().name(), user.getCreatedAt(), images);
        }
    }

    @Data
    @AllArgsConstructor
    public static class ImageInfo {
        private Long id;
        private String filename;
        private LocalDateTime uploadedAt;
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "Имя обязательно")
        @Size(max = 100)
        private String name;

        @Size(max = 500, message = "Описание не более 500 символов")
        private String bio;
    }

    @Data
    public static class ChangeRoleRequest {
        private String role;

        @NotBlank(message = "security-code обязателен для смены роли")
        private String securityCode;
    }
}
