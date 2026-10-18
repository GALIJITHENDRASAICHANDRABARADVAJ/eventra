package com.eventra.repository;

import com.eventra.entity.CinemaScreen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CinemaScreenRepository extends JpaRepository<CinemaScreen, Long> {
    List<CinemaScreen> findByCinemaIdOrderByName(Long cinemaId);

    List<CinemaScreen> findByActiveTrue();
}
