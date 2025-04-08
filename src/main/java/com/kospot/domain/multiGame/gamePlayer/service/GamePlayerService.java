package com.kospot.domain.multiGame.gamePlayer.service;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multiGame.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gamePlayer.repository.GamePlayerRepository;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GamePlayerService {

    private final MemberAdaptor memberAdaptor;
    private final GamePlayerRepository gamePlayerRepository;

    public List<GamePlayer> createGamePlayers(GameRoom gameRoom) {
        List<Member> members = memberAdaptor.queryAllByGameRoomId(gameRoom.getId());
        List<GamePlayer> players = members.stream().map(
                m -> GamePlayer.create(m, gameRoom)).toList();
        return gamePlayerRepository.saveAll(players);
    }
}
