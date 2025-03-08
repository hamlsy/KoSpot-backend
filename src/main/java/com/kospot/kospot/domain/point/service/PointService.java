package com.kospot.kospot.domain.point.service;

import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.entity.RankTier;
import com.kospot.kospot.domain.member.entity.Member;

import com.kospot.kospot.domain.point.util.PointCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PointService {

    public int addPointByRankGameScore(Member member, GameRank gameRank, double score) {
        RankTier tier = gameRank.getRankTier();
        int point = PointCalculator.getRankPoint(tier, score);
        member.addPoint(point);
        return point;
    }

    public int addPointByRankGameScoreV2(Member member, RankTier tier, double score) {
        int point = PointCalculator.getRankPoint(tier, score);
        member.addPoint(point);
        return point;
    }

    public void addPoint(Member member, int amount) {
        member.addPoint(amount);
    }

    public void usePoint(Member member, int amount) {
        member.usePoint(amount);
    }

}
