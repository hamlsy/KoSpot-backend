package com.kospot.domain.multiGame.gamePlayer.service;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GamePlayerService {

    private final MemberAdaptor memberAdaptor;
    private final GamePlayerRepository gamePlayerRepository;

    public List<GamePlayer> createRoadViewGamePlayers(GameRoom gameRoom, MultiRoadViewGame game) {
        List<Member> members = memberAdaptor.queryAllByGameRoomId(gameRoom.getId());
        List<GamePlayer> players = members.stream().map(
                m -> GamePlayer.createRoadViewGamePlayer(m, game)).toList();
        return gamePlayerRepository.saveAll(players);
    }

    //todo implement bulk update
    public List<GamePlayer> updateTotalRank(List<GamePlayer> gamePlayers) {
        // score 로 내림차순 정렬
        gamePlayers.sort((a, b) -> b.getTotalScore() - a.getTotalScore());

        int rank = 1;
        gamePlayers.get(0).updateRoundRank(rank);

        // 이전 플레이어 점수와 같을 경우 동일 순위 부여
        for (int i = 1; i < gamePlayers.size(); i++) {
            GamePlayer currentPlayer = gamePlayers.get(i);
            GamePlayer previousPlayer = gamePlayers.get(i - 1);
            int previousRank = previousPlayer.getRoundRank();
            if (hasSameScoreAsPreviousPlayer(currentPlayer, previousPlayer)) {
                currentPlayer.updateRoundRank(previousRank);
                continue;
            }
            // next rank
            currentPlayer.updateRoundRank(previousRank + 1);
        }
        return gamePlayers;
    }

    private boolean hasSameScoreAsPreviousPlayer(GamePlayer current, GamePlayer previous) {
        return current.getTotalScore() == previous.getTotalScore();
    }
}
