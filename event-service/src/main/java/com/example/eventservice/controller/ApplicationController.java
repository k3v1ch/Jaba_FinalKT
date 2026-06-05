package com.example.eventservice.controller;

import com.example.eventservice.dto.ApplicationDto;
import com.example.eventservice.security.UserPrincipal;
import com.example.eventservice.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationDto.ApplicationResponse> apply(
            @Valid @RequestBody ApplicationDto.CreateApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.apply(request, principal.email(), principal.name()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ApplicationDto.ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(applicationService.getMyApplications(principal.email()));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<ApplicationDto.ApplicationResponse>> getApplicationsForEvent(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(applicationService.getApplicationsForEvent(eventId));
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<ApplicationDto.ApplicationResponse> reviewApplication(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationDto.ReviewRequest request) {
        return ResponseEntity.ok(applicationService.reviewApplication(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        applicationService.cancelApplication(id, principal.email());
        return ResponseEntity.noContent().build();
    }
}
