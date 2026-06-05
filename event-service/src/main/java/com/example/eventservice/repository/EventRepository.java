package com.example.eventservice.repository;

import com.example.eventservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByStatusOrderByEventDateAsc(Event.EventStatus status);
    List<Event> findAllByOrderByEventDateAsc();
}
