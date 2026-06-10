package com.example.auth.controller;

import com.example.auth.dto.UserProfileDto;
import com.example.auth.entity.User;
import com.example.auth.exception.AppException;
import com.example.auth.exception.ErrorCode;
import com.example.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    @Value("${app.admin.security-code}")
    private String adminSecurityCode;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserProfileDto.ProfileResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> UserProfileDto.ProfileResponse.from(u, List.of()))
                .toList();
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileDto.ProfileResponse changeRole(@PathVariable Long id,
                                                     @Valid @RequestBody UserProfileDto.ChangeRoleRequest req) {
        if (!adminSecurityCode.equals(req.getSecurityCode()))
            throw new AppException(ErrorCode.AUTH_SECURITY_CODE_INVALID);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND, "id=" + id));

        try {
            user.setRole(User.Role.valueOf(req.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Unknown role: " + req.getRole());
        }
        userRepository.save(user);
        return UserProfileDto.ProfileResponse.from(user, List.of());
    }

    @Data
    @AllArgsConstructor
    public static class StatsResponse {
        private long totalUsers;
        private long admins;
        private long moderators;
        private long users;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public StatsResponse getStats() {
        List<User> all = userRepository.findAll();
        return new StatsResponse(
                all.size(),
                all.stream().filter(u -> u.getRole() == User.Role.ADMIN).count(),
                all.stream().filter(u -> u.getRole() == User.Role.MODERATOR).count(),
                all.stream().filter(u -> u.getRole() == User.Role.USER).count()
        );
    }
}
