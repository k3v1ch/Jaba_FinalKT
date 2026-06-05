package com.example.eventservice.messaging;

import com.example.eventservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishApplicationApproved(String email, String name, String eventTitle) {
        publish("application.approved", email, name, eventTitle,
                "Ваша заявка на мероприятие \"" + eventTitle + "\" одобрена!");
    }

    public void publishApplicationRejected(String email, String name, String eventTitle) {
        publish("application.rejected", email, name, eventTitle,
                "Ваша заявка на мероприятие \"" + eventTitle + "\" отклонена.");
    }

    public void publishApplicationReceived(String email, String name, String eventTitle) {
        publish("application.received", email, name, eventTitle,
                "Ваша заявка на мероприятие \"" + eventTitle + "\" принята и ожидает рассмотрения.");
    }

    private void publish(String type, String email, String name, String eventTitle, String message) {
        NotificationEvent event = new NotificationEvent(type, email, name, eventTitle, message, LocalDateTime.now());
        String routingKey = RabbitMQConfig.ROUTING_NOTIFICATION + "." + type;
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
        log.info("Published notification event: {} for {}", type, email);
    }
}
