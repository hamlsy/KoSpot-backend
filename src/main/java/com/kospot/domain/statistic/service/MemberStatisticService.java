package com.kospot.domain.statistic.service;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.statistic.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.domain.statistic.repository.MemberStatisticRepository;
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
    public void updateSingleGameStatistic(Member member, GameMode gameMode, GameType gameType, 
                                          double score, LocalDateTime playTime) {
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(member);
        LocalDate playDate = playTime.toLocalDate();
        statistic.updateGameStatistic(gameMode, gameType, score, playDate, playTime);
    }

    @Transactional
    public void updateMultiGameStatistic(Member member, GameMode gameMode, double score, 
                                          Integer finalRank, LocalDateTime playTime) {
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(member);
        LocalDate playDate = playTime.toLocalDate();
        statistic.updateMultiGameStatistic(gameMode, score, finalRank, playDate, playTime);
    }

}

