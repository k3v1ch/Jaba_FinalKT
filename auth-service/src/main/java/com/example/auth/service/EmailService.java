package com.example.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public String buildVerifyLink(String token) {
        return frontendUrl + "/api/auth/verify-email?token=" + token;
    }

    @Async("emailExecutor")
    public void sendVerificationEmail(String toEmail, String token) {
        String link = buildVerifyLink(token);

        log.info("=== EMAIL VERIFICATION ===");
        log.info("To: {}", toEmail);
        log.info("Link: {}", link);
        log.info("==========================");

        if (fromEmail == null || fromEmail.isBlank()) return;

        try {
            String body = """
                    <h2>Подтверждение регистрации</h2>
                    <p>Перейдите по ссылке для активации аккаунта:</p>
                    <a href="%s">Подтвердить email</a>
                    <p style="color:#999;font-size:12px">Ссылка действует 24 часа.</p>
                    """.formatted(link);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(fromEmail);
            h.setTo(toEmail);
            h.setSubject("Подтвердите email — Event Platform");
            h.setText(body, true);
            mailSender.send(msg);
            log.info("EMAIL: sent to {}", toEmail);
        } catch (Exception e) {
            log.error("EMAIL: failed to {}: {}", toEmail, e.getMessage());
        }
    }
}
