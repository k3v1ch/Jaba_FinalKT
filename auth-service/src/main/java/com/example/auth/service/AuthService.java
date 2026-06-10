package com.example.auth.service;

import com.example.auth.dto.AuthDto;
import com.example.auth.entity.*;
import com.example.auth.exception.*;
import com.example.auth.messaging.EventPublisher;
import com.example.auth.repository.*;
import com.example.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final TwoFactorService twoFactorService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    public AuthDto.MessageResponse register(AuthDto.RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email))
            throw new AppException(ErrorCode.AUTH_EMAIL_EXISTS, email);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName().trim())
                .build();
        User saved = userRepository.save(user);

        VerificationToken vt = VerificationToken.builder()
                .user(saved).expiresAt(LocalDateTime.now().plusHours(24)).build();
        VerificationToken savedVt = verificationTokenRepository.save(vt);

        emailService.sendVerificationEmail(email, savedVt.getToken());
        eventPublisher.publishUserEvent("registered", Map.of(
                "email", email,
                "name", saved.getName(),
                "verifyLink", emailService.buildVerifyLink(savedVt.getToken())));

        return new AuthDto.MessageResponse("Регистрация успешна. Подтвердите email.");
    }

    public AuthDto.MessageResponse verifyEmail(String tokenValue) {
        VerificationToken vt = verificationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_TOKEN_INVALID, tokenValue));
        if (vt.isUsed())    throw new AppException(ErrorCode.AUTH_TOKEN_USED);
        if (vt.isExpired()) throw new AppException(ErrorCode.AUTH_TOKEN_INVALID, "expired");

        vt.getUser().setEnabled(true);
        vt.setUsed(true);
        userRepository.save(vt.getUser());
        verificationTokenRepository.save(vt);

        eventPublisher.publishUserEvent("email_verified", Map.of(
                "email", vt.getUser().getEmail(),
                "name", vt.getUser().getName()));
        return new AuthDto.MessageResponse("Email подтверждён.");
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (DisabledException e) {
            // The provider's pre-auth check fires before our isEnabled() check below —
            // without this catch an unverified login surfaces as a 500.
            throw new AppException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        } catch (BadCredentialsException e) {
            throw new AppException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND, email));

        if (!user.isEnabled()) throw new AppException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);

        if (user.isTwoFactorEnabled()) {
            if (request.getTotpCode() == null || request.getTotpCode().isBlank())
                return AuthDto.AuthResponse.twoFactorRequired();
            if (!twoFactorService.validateCode(user.getTwoFactorSecret(), request.getTotpCode()))
                throw new AppException(ErrorCode.AUTH_2FA_INVALID);
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken rt = refreshTokenRepository.save(RefreshToken.builder()
                .user(user).expiresAt(LocalDateTime.now().plusDays(7)).build());

        eventPublisher.publishUserEvent("login", Map.of("email", email, "name", user.getName()));
        return new AuthDto.AuthResponse(accessToken, rt.getToken());
    }

    public AuthDto.AuthResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        RefreshToken rt = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_REFRESH_INVALID));
        if (rt.isRevoked()) throw new AppException(ErrorCode.AUTH_REFRESH_REVOKED);
        if (rt.isExpired()) throw new AppException(ErrorCode.AUTH_REFRESH_INVALID, "expired");
        if (!rt.getUser().isEnabled()) throw new AppException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        return new AuthDto.AuthResponse(jwtService.generateToken(rt.getUser()), rt.getToken());
    }

    public AuthDto.MessageResponse logout(AuthDto.RefreshTokenRequest request) {
        RefreshToken rt = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_REFRESH_INVALID));
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
        return new AuthDto.MessageResponse("Вы вышли из системы.");
    }

    public AuthDto.TwoFactorSetupResponse setupTwoFactor(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND, email));
        // Re-running setup while 2FA is active would silently swap the secret,
        // letting a stolen access token re-enroll 2FA. Require disable (with code) first.
        if (user.isTwoFactorEnabled()) throw new AppException(ErrorCode.AUTH_2FA_ALREADY_ENABLED);
        String secret = twoFactorService.generateSecret();
        user.setTwoFactorSecret(secret);
        userRepository.save(user);
        return new AuthDto.TwoFactorSetupResponse(secret, twoFactorService.generateQrCodeUrl(email, secret));
    }

    public AuthDto.MessageResponse confirmTwoFactor(String email, AuthDto.TwoFactorVerifyRequest req) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND, email));
        if (user.getTwoFactorSecret() == null) throw new AppException(ErrorCode.AUTH_2FA_NOT_SETUP);
        if (!twoFactorService.validateCode(user.getTwoFactorSecret(), req.getCode()))
            throw new AppException(ErrorCode.AUTH_2FA_INVALID);
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        return new AuthDto.MessageResponse("2FA включена.");
    }

    public AuthDto.MessageResponse disableTwoFactor(String email, AuthDto.TwoFactorVerifyRequest req) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND, email));
        // A bare access token must not be enough to strip 2FA — demand a current TOTP code.
        if (user.isTwoFactorEnabled()) {
            if (user.getTwoFactorSecret() == null
                    || !twoFactorService.validateCode(user.getTwoFactorSecret(), req.getCode()))
                throw new AppException(ErrorCode.AUTH_2FA_INVALID);
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        return new AuthDto.MessageResponse("2FA отключена.");
    }
}
