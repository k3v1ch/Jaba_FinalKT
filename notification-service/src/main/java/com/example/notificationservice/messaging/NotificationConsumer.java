package com.example.notificationservice.messaging;

import com.example.notificationservice.config.RabbitMQConfig;
import com.example.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Received notification event: {} for {}", event.getType(), event.getRecipientEmail());

        String subject = switch (event.getType()) {
            case "application.approved" -> "Заявка одобрена — " + event.getEventTitle();
            case "application.rejected" -> "Заявка отклонена — " + event.getEventTitle();
            case "application.received" -> "Заявка принята — " + event.getEventTitle();
            default -> "Уведомление от Event Platform";
        };

        String body = "Здравствуйте, " + event.getRecipientName() + "!\n\n"
                + event.getMessage() + "\n\n"
                + "С уважением,\nEvent Platform";

        emailService.sendEmail(event.getRecipientEmail(), subject, body);
    }

    @RabbitListener(queues = RabbitMQConfig.USER_EVENTS_QUEUE)
    public void handleUserEvent(UserEvent event) {
        log.info("Received user event: {}", event.getType());

        String email = event.getPayload().get("email");
        String name = event.getPayload().get("name");

        if (email == null) return;

        String subject;
        String body;

        switch (event.getType()) {
            case "registered" -> {
                String verifyLink = event.getPayload().getOrDefault("verifyLink", "");
                subject = "Подтвердите email — Event Platform";
                body = "Здравствуйте, " + name + "!\n\n"
                        + "Для подтверждения email перейдите по ссылке:\n"
                        + verifyLink + "\n\n"
                        + "Ссылка действует 24 часа.\n\nС уважением,\nEvent Platform";
            }
            case "email_verified" -> {
                subject = "Email подтверждён — Event Platform";
                body = "Здравствуйте, " + name + "!\n\n"
                        + "Ваш email успешно подтверждён. Теперь вы можете войти в систему.\n\n"
                        + "С уважением,\nEvent Platform";
            }
            default -> {
                log.debug("Unhandled user event type: {}", event.getType());
                return;
            }
        }

        emailService.sendEmail(email, subject, body);
    }
}
