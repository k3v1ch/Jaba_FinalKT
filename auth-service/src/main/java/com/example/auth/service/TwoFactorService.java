package com.example.auth.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.*;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;

    public String generateSecret() { return secretGenerator.generate(); }

    public String generateQrCodeUrl(String email, String secret) {
        QrData data = new QrData.Builder()
                .label(email).secret(secret).issuer("EventPlatform")
                .algorithm(HashingAlgorithm.SHA1).digits(6).period(30)
                .build();
        try {
            return getDataUriForImage(qrGenerator.generate(data), qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new RuntimeException("QR generation failed", e);
        }
    }

    public boolean validateCode(String secret, String code) {
        return new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider())
                .isValidCode(secret, code);
    }
}
