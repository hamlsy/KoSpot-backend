package com.kospot.domain.multi.game.repository;

import com.kospot.domain.multi.game.entity.MultiPhotoGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MultiPhotoGameRepository extends JpaRepository<MultiPhotoGame, Long> {

} 