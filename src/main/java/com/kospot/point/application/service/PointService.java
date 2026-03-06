package com.kospot.point.application.service;

import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.member.domain.entity.Member;

import com.kospot.point.common.utils.PointCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PointService {

    public int addPointByRankGameScore(Member member, RankTier tier, double score) {
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
