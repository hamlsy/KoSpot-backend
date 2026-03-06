package com.kospot.statistic.application.service;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.game.domain.vo.GameType;
import com.kospot.statistic.application.adaptor.MemberStatisticAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.statistic.domain.entity.MemberStatistic;
import com.kospot.statistic.infrastructure.persistence.MemberStatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberStatisticService {

    private final MemberStatisticAdaptor memberStatisticAdaptor;
    private final MemberStatisticRepository memberStatisticRepository;


    public void initializeStatistic(Member member) {
        MemberStatistic statistic = MemberStatistic.create(member);
        memberStatisticRepository.save(statistic);
    }


    public void updateSingleGameStatistic(Member member, GameMode gameMode, GameType gameType, 
                                          double score, LocalDateTime playTime) {
        MemberStatistic statistic = memberStatisticAdaptor.queryByMemberFetchModeStatistics(member);
        LocalDate playDate = playTime.toLocalDate();
        statistic.updateGameStatistic(gameMode, gameType, score, playDate, playTime);
    }


    public void updateMultiGameStatistic(Member member, GameMode gameMode, double score, 
                                          Integer finalRank, LocalDateTime playTime) {
        MemberStatistic statistic = memberStatisticAdaptor.queryByMemberFetchModeStatistics(member);
        LocalDate playDate = playTime.toLocalDate();
        statistic.updateMultiGameStatistic(gameMode, score, finalRank, playDate, playTime);
    }

}

