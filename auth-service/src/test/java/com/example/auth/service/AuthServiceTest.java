package com.example.auth.service;

import com.example.auth.dto.AuthDto;
import com.example.auth.entity.*;
import com.example.auth.exception.*;
import com.example.auth.messaging.EventPublisher;
import com.example.auth.repository.*;
import com.example.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock VerificationTokenRepository verificationTokenRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService jwtService;
    @Mock EmailService emailService;
    @Mock TwoFactorService twoFactorService;
    @Mock AuthenticationManager authenticationManager;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EventPublisher eventPublisher;

    @InjectMocks AuthService authService;

    @Test
    void register_duplicateEmail_throws() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setName("Test");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getCode())
                        .isEqualTo(ErrorCode.AUTH_EMAIL_EXISTS));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_newUser_savesAndSendsEmail() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setEmail("NEW@EXAMPLE.COM");
        req.setPassword("password123");
        req.setName("New User");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> { User u = inv.getArgument(0); u.setId(1L); return u; });
        when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(emailService.buildVerifyLink(any())).thenReturn("http://localhost/verify?token=abc");

        var result = authService.register(req);

        assertThat(result.getMessage()).contains("Регистрация");
        verify(emailService).sendVerificationEmail(eq("new@example.com"), any());
        verify(eventPublisher).publishUserEvent(eq("registered"), anyMap());
    }

    @Test
    void verifyEmail_usedToken_throws() {
        VerificationToken vt = VerificationToken.builder()
                .used(true).expiresAt(LocalDateTime.now().plusHours(1)).build();
        when(verificationTokenRepository.findByToken("tok")).thenReturn(Optional.of(vt));

        assertThatThrownBy(() -> authService.verifyEmail("tok"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getCode())
                        .isEqualTo(ErrorCode.AUTH_TOKEN_USED));
    }

    @Test
    void verifyEmail_validToken_enablesUser() {
        User user = User.builder().email("u@e.com").password("pw").name("U").build();
        VerificationToken vt = VerificationToken.builder()
                .used(false).expiresAt(LocalDateTime.now().plusHours(1)).user(user).build();
        when(verificationTokenRepository.findByToken("good")).thenReturn(Optional.of(vt));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.verifyEmail("good");

        assertThat(user.isEnabled()).isTrue();
        assertThat(vt.isUsed()).isTrue();
    }

    @Test
    void login_unverifiedEmail_throws() {
        User user = User.builder().email("u@e.com").password("pw").name("U").enabled(false).build();
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);

        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("u@e.com");
        req.setPassword("pw");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getCode())
                        .isEqualTo(ErrorCode.AUTH_EMAIL_NOT_VERIFIED));
    }
}
