package com.example.eventservice.dto;

import com.example.eventservice.entity.Event;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

public class EventDto {

    @Data
    public static class CreateEventRequest {
        @NotBlank(message = "Название обязательно")
        @Size(max = 200)
        private String title;

        @Size(max = 2000)
        private String description;

        @NotNull(message = "Дата мероприятия обязательна")
        @Future(message = "Дата должна быть в будущем")
        private LocalDateTime eventDate;

        @Min(value = 1, message = "Минимум 1 участник")
        @Max(value = 10000)
        private int maxParticipants;

        @NotNull(message = "Тип мероприятия обязателен")
        private Event.EventType type;
    }

    @Data
    public static class UpdateEventRequest {
        @Size(max = 200)
        private String title;

        @Size(max = 2000)
        private String description;

        private LocalDateTime eventDate;
        private Integer maxParticipants;
        private Event.EventType type;
        private Event.EventStatus status;
    }

    @Data
    @AllArgsConstructor
    public static class EventResponse {
        private Long id;
        private String title;
        private String description;
        private LocalDateTime eventDate;
        private int maxParticipants;
        private long approvedCount;
        private Event.EventType type;
        private Event.EventStatus status;
        private LocalDateTime createdAt;
    }
}
