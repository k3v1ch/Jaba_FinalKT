package com.example.eventservice.service;

import com.example.eventservice.dto.ApplicationDto;
import com.example.eventservice.entity.Application;
import com.example.eventservice.entity.Event;
import com.example.eventservice.exception.AppException;
import com.example.eventservice.exception.ErrorCode;
import com.example.eventservice.messaging.EventPublisher;
import com.example.eventservice.repository.ApplicationRepository;
import com.example.eventservice.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final EventRepository eventRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ApplicationDto.ApplicationResponse apply(ApplicationDto.CreateApplicationRequest request,
                                                     String userEmail, String userName) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));

        if (event.getStatus() != Event.EventStatus.OPEN) {
            throw new AppException(ErrorCode.EVENT_CLOSED);
        }

        if (applicationRepository.existsByEventIdAndUserEmail(event.getId(), userEmail)) {
            throw new AppException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }

        long approved = applicationRepository.countByEventIdAndStatus(event.getId(), Application.ApplicationStatus.APPROVED);
        if (approved >= event.getMaxParticipants()) {
            throw new AppException(ErrorCode.EVENT_FULL);
        }

        Application app = Application.builder()
                .event(event)
                .userEmail(userEmail)
                .userName(userName)
                .comment(request.getComment())
                .build();

        Application saved = applicationRepository.save(app);
        eventPublisher.publishApplicationReceived(userEmail, userName, event.getTitle());
        return toResponse(saved);
    }

    public List<ApplicationDto.ApplicationResponse> getMyApplications(String userEmail) {
        return applicationRepository.findAllByUserEmailOrderByAppliedAtDesc(userEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public List<ApplicationDto.ApplicationResponse> getApplicationsForEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new AppException(ErrorCode.EVENT_NOT_FOUND);
        }
        return applicationRepository.findAllByEventIdOrderByAppliedAtDesc(eventId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Transactional
    public ApplicationDto.ApplicationResponse reviewApplication(Long applicationId,
                                                                 ApplicationDto.ReviewRequest request) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        if (app.getStatus() != Application.ApplicationStatus.PENDING) {
            throw new AppException(ErrorCode.APPLICATION_NOT_PENDING);
        }

        if (request.getStatus() == Application.ApplicationStatus.APPROVED) {
            long approved = applicationRepository.countByEventIdAndStatus(
                    app.getEvent().getId(), Application.ApplicationStatus.APPROVED);
            if (approved >= app.getEvent().getMaxParticipants()) {
                throw new AppException(ErrorCode.EVENT_FULL);
            }
        }

        app.setStatus(request.getStatus());
        app.setReviewComment(request.getReviewComment());
        Application saved = applicationRepository.save(app);

        if (request.getStatus() == Application.ApplicationStatus.APPROVED) {
            eventPublisher.publishApplicationApproved(app.getUserEmail(), app.getUserName(), app.getEvent().getTitle());
        } else if (request.getStatus() == Application.ApplicationStatus.REJECTED) {
            eventPublisher.publishApplicationRejected(app.getUserEmail(), app.getUserName(), app.getEvent().getTitle());
        }

        return toResponse(saved);
    }

    @Transactional
    public void cancelApplication(Long applicationId, String userEmail) {
        Application app = applicationRepository.findByIdAndUserEmail(applicationId, userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        if (app.getStatus() != Application.ApplicationStatus.PENDING) {
            throw new AppException(ErrorCode.APPLICATION_NOT_PENDING);
        }

        applicationRepository.delete(app);
    }

    private ApplicationDto.ApplicationResponse toResponse(Application app) {
        return new ApplicationDto.ApplicationResponse(
                app.getId(),
                app.getEvent().getId(),
                app.getEvent().getTitle(),
                app.getUserEmail(),
                app.getUserName(),
                app.getStatus(),
                app.getComment(),
                app.getReviewComment(),
                app.getAppliedAt(),
                app.getUpdatedAt()
        );
    }
}
