package com.example.notificationservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String type;
    private Map<String, String> payload;
    private LocalDateTime occurredAt;
}
