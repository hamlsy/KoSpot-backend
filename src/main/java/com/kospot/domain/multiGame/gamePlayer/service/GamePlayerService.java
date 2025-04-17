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

    //todo refactoring
    public List<GamePlayer> updateTotalRank(List<GamePlayer> gamePlayers) {
        gamePlayers.sort((a, b) -> b.getTotalScore() - a.getTotalScore());

        int rank = 1;
        int sameRankCount = 1;
        int previousScore = gamePlayers.get(0).getTotalScore();

        for (int i = 0; i < gamePlayers.size(); i++) {
            GamePlayer currentPlayer = gamePlayers.get(i);
            int currentScore = currentPlayer.getTotalScore();

            if (currentScore < previousScore) {
                rank = i + 1;
                sameRankCount = 1;
            } else {
                sameRankCount++;
            }

            currentPlayer.updateRoundRank(rank);
            previousScore = currentScore;
        }

        return gamePlayers;
    }

}
