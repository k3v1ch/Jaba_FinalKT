package com.example.eventservice.service;

import com.example.eventservice.dto.ApplicationDto;
import com.example.eventservice.entity.Application;
import com.example.eventservice.entity.Event;
import com.example.eventservice.exception.AppException;
import com.example.eventservice.exception.ErrorCode;
import com.example.eventservice.messaging.EventPublisher;
import com.example.eventservice.repository.ApplicationRepository;
import com.example.eventservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ApplicationService applicationService;

    private Event openEvent;

    @BeforeEach
    void setUp() {
        openEvent = Event.builder()
                .id(1L)
                .title("Test Conference")
                .eventDate(LocalDateTime.now().plusDays(10))
                .maxParticipants(100)
                .type(Event.EventType.CONFERENCE)
                .status(Event.EventStatus.OPEN)
                .createdBy("admin@test.com")
                .build();
    }

    @Test
    void apply_eventNotFound_throwsException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new ApplicationDto.CreateApplicationRequest();
        request.setEventId(99L);

        assertThatThrownBy(() -> applicationService.apply(request, "user@test.com", "User"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EVENT_NOT_FOUND));
    }

    @Test
    void apply_closedEvent_throwsException() {
        openEvent.setStatus(Event.EventStatus.CLOSED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(openEvent));

        var request = new ApplicationDto.CreateApplicationRequest();
        request.setEventId(1L);

        assertThatThrownBy(() -> applicationService.apply(request, "user@test.com", "User"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EVENT_CLOSED));
    }

    @Test
    void apply_alreadyApplied_throwsException() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(openEvent));
        when(applicationRepository.existsByEventIdAndUserEmail(1L, "user@test.com")).thenReturn(true);

        var request = new ApplicationDto.CreateApplicationRequest();
        request.setEventId(1L);

        assertThatThrownBy(() -> applicationService.apply(request, "user@test.com", "User"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.APPLICATION_ALREADY_EXISTS));
    }

    @Test
    void apply_success_savesApplicationAndPublishesEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(openEvent));
        when(applicationRepository.existsByEventIdAndUserEmail(1L, "user@test.com")).thenReturn(false);
        when(applicationRepository.countByEventIdAndStatus(1L, Application.ApplicationStatus.APPROVED)).thenReturn(5L);

        Application saved = Application.builder()
                .id(1L)
                .event(openEvent)
                .userEmail("user@test.com")
                .userName("User")
                .status(Application.ApplicationStatus.PENDING)
                .build();
        when(applicationRepository.save(any())).thenReturn(saved);

        var request = new ApplicationDto.CreateApplicationRequest();
        request.setEventId(1L);

        var response = applicationService.apply(request, "user@test.com", "User");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Application.ApplicationStatus.PENDING);
        verify(eventPublisher).publishApplicationReceived("user@test.com", "User", "Test Conference");
    }

    @Test
    void reviewApplication_notPending_throwsException() {
        Application approved = Application.builder()
                .id(1L)
                .event(openEvent)
                .userEmail("user@test.com")
                .userName("User")
                .status(Application.ApplicationStatus.APPROVED)
                .build();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(approved));

        var reviewRequest = new ApplicationDto.ReviewRequest();
        reviewRequest.setStatus(Application.ApplicationStatus.REJECTED);

        assertThatThrownBy(() -> applicationService.reviewApplication(1L, reviewRequest))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.APPLICATION_NOT_PENDING));
    }

    @Test
    void reviewApplication_approve_publishesApprovalEvent() {
        Application pending = Application.builder()
                .id(1L)
                .event(openEvent)
                .userEmail("user@test.com")
                .userName("User")
                .status(Application.ApplicationStatus.PENDING)
                .build();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(applicationRepository.countByEventIdAndStatus(1L, Application.ApplicationStatus.APPROVED)).thenReturn(0L);
        when(applicationRepository.save(any())).thenReturn(pending);

        var reviewRequest = new ApplicationDto.ReviewRequest();
        reviewRequest.setStatus(Application.ApplicationStatus.APPROVED);

        applicationService.reviewApplication(1L, reviewRequest);

        verify(eventPublisher).publishApplicationApproved("user@test.com", "User", "Test Conference");
    }
}
