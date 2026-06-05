package com.example.eventservice.service;

import com.example.eventservice.dto.EventDto;
import com.example.eventservice.entity.Event;
import com.example.eventservice.exception.AppException;
import com.example.eventservice.exception.ErrorCode;
import com.example.eventservice.repository.ApplicationRepository;
import com.example.eventservice.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventManagementService {

    private final EventRepository eventRepository;
    private final ApplicationRepository applicationRepository;

    public List<EventDto.EventResponse> getAllEvents() {
        return eventRepository.findAllByOrderByEventDateAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<EventDto.EventResponse> getOpenEvents() {
        return eventRepository.findAllByStatusOrderByEventDateAsc(Event.EventStatus.OPEN)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EventDto.EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));
        return toResponse(event);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Transactional
    public EventDto.EventResponse createEvent(EventDto.CreateEventRequest request, String createdBy) {
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .maxParticipants(request.getMaxParticipants())
                .type(request.getType())
                .createdBy(createdBy)
                .build();
        return toResponse(eventRepository.save(event));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Transactional
    public EventDto.EventResponse updateEvent(Long id, EventDto.UpdateEventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getMaxParticipants() != null) event.setMaxParticipants(request.getMaxParticipants());
        if (request.getType() != null) event.setType(request.getType());
        if (request.getStatus() != null) event.setStatus(request.getStatus());

        return toResponse(eventRepository.save(event));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new AppException(ErrorCode.EVENT_NOT_FOUND);
        }
        eventRepository.deleteById(id);
    }

    private EventDto.EventResponse toResponse(Event event) {
        long approvedCount = applicationRepository.countByEventIdAndStatus(
                event.getId(), com.example.eventservice.entity.Application.ApplicationStatus.APPROVED);
        return new EventDto.EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getMaxParticipants(),
                approvedCount,
                event.getType(),
                event.getStatus(),
                event.getCreatedBy(),
                event.getCreatedAt()
        );
    }
}
