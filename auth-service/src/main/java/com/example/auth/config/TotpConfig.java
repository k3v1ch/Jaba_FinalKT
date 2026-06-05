package com.example.auth.config;

import dev.samstevens.totp.qr.*;
import dev.samstevens.totp.secret.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TotpConfig {
    @Bean public SecretGenerator secretGenerator() { return new DefaultSecretGenerator(); }
    @Bean public QrGenerator qrGenerator()         { return new ZxingPngQrGenerator(); }
}
