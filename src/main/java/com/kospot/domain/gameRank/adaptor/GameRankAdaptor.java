package com.kospot.domain.gameRank.adaptor;

import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.gameRank.entity.GameRank;
import com.kospot.domain.gameRank.repository.GameRankRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class GameRankAdaptor {

    private final GameRankRepository repository;

    public GameRank queryByMemberAndGameMode(Member member, GameMode gameMode) {
        return repository.findByMemberAndGameMode(member, gameMode);
    }

}
