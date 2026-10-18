package com.eventra.repository;

import com.eventra.entity.MovieCast;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieCastRepository extends JpaRepository<MovieCast, Long> {
}
