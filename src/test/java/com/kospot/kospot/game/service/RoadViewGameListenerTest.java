package com.kospot.kospot.game.service;

import com.kospot.kospot.application.game.roadView.rank.EndRoadViewRankUseCase;
import com.kospot.kospot.application.game.roadView.rank.EndRoadViewRankUseCaseV2;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.repository.GameRankRepository;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Transactional
public class RoadViewGameListenerTest {

    @Autowired
    private EndRoadViewRankUseCase endRoadViewRankUseCase;

    @Autowired
    private EndRoadViewRankUseCaseV2 endRoadViewRankUseCaseV2;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRankRepository gameRankRepository;

    @Autowired
    private RoadViewGameService gameService;

    @Autowired
    private RoadViewGameRepository roadViewGameRepository;

    private List<Member> members = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // given
        for(int i = 0 ; i < 100; i ++) {
            Member member = Member.builder()
                    .nickname("nickname" + i)
                    .username("username" + i)
                    .build();
            members.add(member);
            GameRank gameRank = GameRank.create(member, GameType.ROADVIEW);
            gameRankRepository.save(gameRank);
            RoadViewGame game = gameService.startRankGame(member);
            roadViewGameRepository.save(game);
        }
        // when
        memberRepository.saveAll(members);

        // then
    }

    @Test
    @DisplayName("기존 게임 종료 로직 시간 측정")
    void testGameEnd() {
        // given
        // when
        // then
    }

    @Test
    @DisplayName("게임 종료 이벤트 리스너 시간 측정")
    void testGameEndEventListener() {
        //given
        //when
        //then
    }
}
