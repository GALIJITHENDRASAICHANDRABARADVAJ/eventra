package com.eventra.entity;

import com.eventra.entity.enums.FavoriteType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "favorites")
public class Favorite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Enumerated(EnumType.STRING)
    private FavoriteType targetType;
    private Long targetId;
    private Instant createdAt = Instant.now();
}
