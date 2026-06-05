package com.example.eventservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String type;
    private String recipientEmail;
    private String recipientName;
    private String eventTitle;
    private String message;
    private LocalDateTime occurredAt;
}
