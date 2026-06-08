package com.example.auth.controller;

import com.example.auth.dto.AuthDto;
import com.example.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/register")
    public AuthDto.MessageResponse register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        return authService.register(req);
    }

    // Opened from an email link in a browser — redirect to the SPA with a result
    // flag instead of returning raw JSON.
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        boolean ok;
        try {
            authService.verifyEmail(token);
            ok = true;
        } catch (Exception e) {
            ok = false;
        }
        URI target = URI.create(frontendUrl + "/?verified=" + (ok ? "1" : "0") + "#/login");
        return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
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

    // 2FA always targets the CURRENT user — take the email from the authenticated
    // principal, never from a request param (which would let one user touch another's 2FA).
    @PostMapping("/2fa/setup")
    public AuthDto.TwoFactorSetupResponse setup2fa(@AuthenticationPrincipal UserDetails user) {
        return authService.setupTwoFactor(user.getUsername());
    }

    @PostMapping("/2fa/confirm")
    public AuthDto.MessageResponse confirm2fa(@AuthenticationPrincipal UserDetails user,
                                              @Valid @RequestBody AuthDto.TwoFactorVerifyRequest req) {
        return authService.confirmTwoFactor(user.getUsername(), req);
    }

    @PostMapping("/2fa/disable")
    public AuthDto.MessageResponse disable2fa(@AuthenticationPrincipal UserDetails user) {
        return authService.disableTwoFactor(user.getUsername());
    }
}
