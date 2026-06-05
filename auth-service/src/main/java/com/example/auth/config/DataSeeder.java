package com.example.auth.config;

import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email:admin@event.com}")
    private String adminEmail;

    @Value("${app.seed.admin-password:Admin1234}")
    private String adminPassword;

    @Value("${app.seed.admin-name:Admin}")
    private String adminName;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) return;

        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .name(adminName)
                .enabled(true)
                .role(User.Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("==============================================");
        log.info("  DEV ADMIN CREATED");
        log.info("  Email:    {}", adminEmail);
        log.info("  Password: {}", adminPassword);
        log.info("==============================================");
    }
}
