package com.kospot.domain.multi.room.repository;

import com.kospot.domain.multi.room.entity.GameRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {

//    @Query("select gr from GameRoom gr left join fetch gr.waitingPlayers where gr.id = :id")
//    Optional<GameRoom> findByIdFetchPlayers(@Param("id") Long id);

    @Query("select gr from GameRoom gr join fetch gr.host where gr.id = :id")
    Optional<GameRoom> findByIdFetchHost(@Param("id") Long id);

    //todo search refactoring -> paging, fetch join 같이 사용 x
    @Query("select gr from GameRoom gr join fetch gr.host h where gr.title like CONCAT('%', :keyword, '%') ")
    List<GameRoom> findAllByKeywordPaging(@Param("keyword") String keyword, Pageable pageable);

    @Query("select gr from GameRoom gr join fetch gr.host h")
    List<GameRoom> findAllPaging(Pageable pageable);

    @EntityGraph(attributePaths = {"host"})
    @Query("select gr from GameRoom gr " +
            "order by " +
            "case when gr.status = 'WAITING' then 0 else 1 end, gr.createdDate desc")
    List<GameRoom> findAllWithWaitingFirst(Pageable pageable);

}
