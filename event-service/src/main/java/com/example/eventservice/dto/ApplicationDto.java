package com.example.eventservice.dto;

import com.example.eventservice.entity.Application;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

public class ApplicationDto {

    @Data
    public static class CreateApplicationRequest {
        @NotNull(message = "ID мероприятия обязателен")
        private Long eventId;

        @Size(max = 500)
        private String comment;
    }

    @Data
    public static class ReviewRequest {
        @NotNull(message = "Статус обязателен")
        private Application.ApplicationStatus status;

        @Size(max = 500)
        private String reviewComment;
    }

    @Data
    @AllArgsConstructor
    public static class ApplicationResponse {
        private Long id;
        private Long eventId;
        private String eventTitle;
        private String userEmail;
        private String userName;
        private Application.ApplicationStatus status;
        private String comment;
        private String reviewComment;
        private LocalDateTime appliedAt;
        private LocalDateTime updatedAt;
    }
}
