package com.eventra.repository;

import com.eventra.entity.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSessionRepository extends JpaRepository<EventSession, Long> {
}
