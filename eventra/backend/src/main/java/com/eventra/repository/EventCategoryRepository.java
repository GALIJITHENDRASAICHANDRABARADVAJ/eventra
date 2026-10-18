package com.eventra.repository;

import com.eventra.entity.EventCategory;
import com.eventra.entity.enums.EventVertical;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {
    List<EventCategory> findByVerticalOrderByName(EventVertical vertical);
    Optional<EventCategory> findByCode(String code);
}
