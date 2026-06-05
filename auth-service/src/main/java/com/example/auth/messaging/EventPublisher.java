package com.example.auth.messaging;

import com.example.auth.config.RabbitMQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public void publishUserEvent(String type, Map<String, String> payload) {
        UserEvent event = new UserEvent(type, payload, LocalDateTime.now());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_USER + "." + type,
                    event);
            log.info("EVENT: {} for {}", type, payload.get("email"));
        } catch (Exception e) {
            log.error("EVENT publish failed {}: {}", type, e.getMessage());
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UserEvent {
        private String type;
        private Map<String, String> payload;
        private LocalDateTime occurredAt;
    }
}
