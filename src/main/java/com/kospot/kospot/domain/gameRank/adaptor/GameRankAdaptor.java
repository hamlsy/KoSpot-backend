package com.kospot.kospot.domain.gameRank.adaptor;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.repository.GameRankRepository;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class GameRankAdaptor {

    private final GameRankRepository repository;

    public GameRank queryByMemberAndGameType(Member member, GameType gameType) {
        return repository.findByMemberAndGameType(member, gameType);
    }

}
