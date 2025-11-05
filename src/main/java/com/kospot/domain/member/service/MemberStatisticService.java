package com.kospot.domain.member.service;

import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.member.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.MemberStatistic;
import com.kospot.domain.member.repository.MemberStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberStatisticService {

    private final MemberStatisticAdaptor memberStatisticAdaptor;
    private final MemberStatisticRepository memberStatisticRepository;

    @Transactional
    public void initializeStatistic(Member member) {
        MemberStatistic statistic = MemberStatistic.create(member);
        memberStatisticRepository.save(statistic);
    }

    @Transactional
    public void updateSingleGameStatistic(Member member, GameType gameType, double score, LocalDateTime playTime) {
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(member);
        LocalDate playDate = playTime.toLocalDate();

        if (gameType == GameType.PRACTICE) {
            statistic.updateSinglePracticeGame(score, playDate, playTime);
        } else if (gameType == GameType.RANK) {
            statistic.updateSingleRankGame(score, playDate, playTime);
        }
    }

    @Transactional
    public void updateMultiGameStatistic(Member member, double score, Integer finalRank, LocalDateTime playTime) {
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(member);
        LocalDate playDate = playTime.toLocalDate();
        statistic.updateMultiGame(score, finalRank, playDate, playTime);
    }
}

