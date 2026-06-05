package com.example.auth.controller;

import com.example.auth.dto.AuthDto;
import com.example.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthDto.MessageResponse register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        return authService.register(req);
    }

    @GetMapping("/verify-email")
    public AuthDto.MessageResponse verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }

    @PostMapping("/login")
    public AuthDto.AuthResponse login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public AuthDto.AuthResponse refresh(@Valid @RequestBody AuthDto.RefreshTokenRequest req) {
        return authService.refreshToken(req);
    }

    @PostMapping("/logout")
    public AuthDto.MessageResponse logout(@Valid @RequestBody AuthDto.RefreshTokenRequest req) {
        return authService.logout(req);
    }

    @PostMapping("/2fa/setup")
    public AuthDto.TwoFactorSetupResponse setup2fa(@RequestParam String email) {
        return authService.setupTwoFactor(email);
    }

    @PostMapping("/2fa/confirm")
    public AuthDto.MessageResponse confirm2fa(@RequestParam String email,
                                              @Valid @RequestBody AuthDto.TwoFactorVerifyRequest req) {
        return authService.confirmTwoFactor(email, req);
    }

    @PostMapping("/2fa/disable")
    public AuthDto.MessageResponse disable2fa(@RequestParam String email) {
        return authService.disableTwoFactor(email);
    }
}
