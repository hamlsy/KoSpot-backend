package com.kospot.kospot.domain.game.repository;

import com.kospot.kospot.domain.game.entity.PhotoGame;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoGameRepository extends JpaRepository<PhotoGame, Long> {
}
