package com.kospot.kospot.domain.multiplay.gameRoom.repository;

import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
}
