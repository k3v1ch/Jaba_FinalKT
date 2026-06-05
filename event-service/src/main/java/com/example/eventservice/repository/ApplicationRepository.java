package com.example.eventservice.repository;

import com.example.eventservice.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findAllByUserEmailOrderByAppliedAtDesc(String userEmail);
    List<Application> findAllByEventIdOrderByAppliedAtDesc(Long eventId);
    boolean existsByEventIdAndUserEmail(Long eventId, String userEmail);
    long countByEventIdAndStatus(Long eventId, Application.ApplicationStatus status);
    Optional<Application> findByIdAndUserEmail(Long id, String userEmail);
}
