package com.kospot.multi.game.infrastructure.persistence;

import com.kospot.multi.game.domain.entity.MultiPhotoGame;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MultiPhotoGameRepository extends JpaRepository<MultiPhotoGame, Long> {

} 