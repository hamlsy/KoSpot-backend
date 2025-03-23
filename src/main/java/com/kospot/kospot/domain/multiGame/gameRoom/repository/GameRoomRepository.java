package com.kospot.kospot.domain.multiGame.gameRoom.repository;

import com.kospot.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.kospot.presentation.multiGame.gameRoom.dto.response.FindGameRoomResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {

    @Query("select gr from GameRoom gr join fetch gr.waitingPlayers where gr.id = :id")
    Optional<GameRoom> findByIdFetchPlayers(@Param("id") Long id);

    //todo search refactoring
    //todo paging
    @Query("select new com.kospot.kospot.presentation.multiGame.dto.response.FindGameRoomResponse(" +
            "gr.id, gr.title, gr.gameMode, gr.gameType, gr.maxPlayers, COUNT(gr.waitingPlayers), h.nickname, gr.privateRoom) " +
            "from GameRoom gr left join gr.host h where gr.title like %:keyword:% ")
    List<FindGameRoomResponse> findAllByKeywordPaging(@Param("keyword") String keyword, Pageable pageable);

}
