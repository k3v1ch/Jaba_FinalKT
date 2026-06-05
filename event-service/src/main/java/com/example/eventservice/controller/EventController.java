package com.example.eventservice.controller;

import com.example.eventservice.dto.EventDto;
import com.example.eventservice.security.UserPrincipal;
import com.example.eventservice.service.EventManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventManagementService eventManagementService;

    @GetMapping
    public ResponseEntity<List<EventDto.EventResponse>> getOpenEvents() {
        return ResponseEntity.ok(eventManagementService.getOpenEvents());
    }

    @GetMapping("/all")
    public ResponseEntity<List<EventDto.EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventManagementService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDto.EventResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventManagementService.getEventById(id));
    }

    @PostMapping
    public ResponseEntity<EventDto.EventResponse> createEvent(
            @Valid @RequestBody EventDto.CreateEventRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventManagementService.createEvent(request, principal.email()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDto.EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventDto.UpdateEventRequest request) {
        return ResponseEntity.ok(eventManagementService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventManagementService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
