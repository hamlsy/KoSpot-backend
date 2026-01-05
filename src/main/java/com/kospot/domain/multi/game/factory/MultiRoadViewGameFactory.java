package com.kospot.domain.multi.game.factory;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.repository.MultiRoadViewGameRepository;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multi.room.entity.GameRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 멀티 로드뷰 게임과 관련 엔티티 생성을 담당하는 Factory
 * 게임 생성과 플레이어 생성 로직을 캡슐화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MultiRoadViewGameFactory {

    private final MemberAdaptor memberAdaptor;
    private final MultiRoadViewGameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * 게임과 플레이어를 함께 생성한다.
     * GameRoom의 설정값을 기반으로 게임을 생성하고,
     * 방에 참여한 멤버들을 GamePlayer로 변환한다.
     *
     * @param gameRoom 게임방 엔티티
     * @return 생성된 게임과 플레이어 목록을 담은 결과 객체
     */
    public GameCreationResult createGameWithPlayers(GameRoom gameRoom) {
        // 게임 생성
        MultiRoadViewGame game = createGame(gameRoom);

        // 방에 참여한 멤버 조회
        List<Member> members = memberAdaptor.queryAllByGameRoomId(gameRoom.getId());

        // 플레이어 생성
        List<GamePlayer> players = createPlayers(members, game);

        log.info("Created game with players - GameId: {}, PlayerCount: {}",
                game.getId(), players.size());

        return GameCreationResult.of(game, players);
    }

    /**
     * 게임만 단독으로 생성한다.
     * 플레이어 생성이 별도로 필요한 경우 사용한다.
     *
     * @param gameRoom 게임방 엔티티
     * @return 생성된 게임 엔티티
     */
    public MultiRoadViewGame createGame(GameRoom gameRoom) {
        PlayerMatchType matchType = gameRoom.getPlayerMatchType();
        int totalRounds = gameRoom.getTotalRounds();
        int timeLimit = gameRoom.getTimeLimit();
        boolean isPoiNameVisible = gameRoom.isPoiNameVisible();

        MultiRoadViewGame game = MultiRoadViewGame.createGame(
                gameRoom.getId(),
                matchType,
                isPoiNameVisible,
                totalRounds,
                timeLimit
        );

        return gameRepository.save(game);
    }

    /**
     * 멤버 목록을 GamePlayer로 변환하여 저장한다.
     *
     * @param members 멤버 목록
     * @param game    게임 엔티티
     * @return 생성된 플레이어 목록
     */
    private List<GamePlayer> createPlayers(List<Member> members, MultiRoadViewGame game) {
        List<GamePlayer> players = members.stream()
                .map(member -> GamePlayer.createRoadViewGamePlayer(member, game))
                .toList();

        return gamePlayerRepository.saveAll(players);
    }
}

