package com.kospot.domain.multi.game.repository;

import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MultiRoadViewGameRepository extends JpaRepository<MultiRoadViewGame, Long> {

    List<MultiRoadViewGame> findAllByGameRoomId(Long id);


}