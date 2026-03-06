package com.kospot.game.infrastructure.persistence;

import com.kospot.game.domain.entity.PhotoGame;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoGameRepository extends JpaRepository<PhotoGame, Long> {
}
