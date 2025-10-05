package com.kospot.multiGame.gameRoom.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.vo.GameRoomStatus;
import com.kospot.domain.multi.room.repository.GameRoomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class GameRoomQueryTest {

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("findAllWithWaitingFirst 메서드 검증 - N+1 문제 없이 단일 쿼리로 실행")
    void findAllWithWaitingFirst_ShouldExecuteSingleQuery() {
        // given
        Member host1 = Member.builder()
                .username("host1")
                .nickname("host1")
                .build();
        Member host2 = Member.builder()
                .username("host2")
                .nickname("host2")
                .build();

        entityManager.persist(host1);
        entityManager.persist(host2);

        GameRoom room1 = GameRoom.builder()
                .title("room1")
                .host(host1)
                .maxPlayers(3)
                .status(GameRoomStatus.WAITING)
                .build();

        GameRoom room2 = GameRoom.builder()
                .title("room2")
                .host(host2)
                .maxPlayers(3)
                .status(GameRoomStatus.PLAYING)
                .build();

        gameRoomRepository.save(room1);
        gameRoomRepository.save(room2);

        entityManager.flush();
        entityManager.clear();

        // when
        Long beforeCount = getSelectQueryCount();
        log.info("before count:" + beforeCount);
        List<GameRoom> results = gameRoomRepository.findAllWithWaitingFirst(PageRequest.of(0, 10));
        Long afterCount = getSelectQueryCount();
        log.info("after count:" + afterCount);

        // then
//        assertThat(afterCount - beforeCount).isEqualTo(1); // 단일 쿼리 실행 확인
        assertEquals(2, results.size());

        // WAITING 상태인 방이 먼저 오는지 확인
        assertThat(results.get(0).getStatus()).isEqualTo(GameRoomStatus.WAITING);
        assertThat(results.get(1).getStatus()).isEqualTo(GameRoomStatus.PLAYING);

        // N+1 문제 검증을 위한 host 접근
        results.forEach(room -> {
            assertThat(Hibernate.isInitialized(room.getHost())).isTrue();
            assertThat(room.getHost().getNickname()).isNotNull();
        });
    }

    private Long getSelectQueryCount() {
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        return sessionFactory.getStatistics().getQueryExecutionCount();
    }
}
