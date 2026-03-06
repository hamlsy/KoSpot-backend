package com.kospot.domain.statistic.adaptor;

import com.kospot.member.domain.entity.Member;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.member.domain.exception.MemberErrorStatus;
import com.kospot.domain.statistic.repository.MemberStatisticRepository;
import com.kospot.common.annotation.adaptor.Adaptor;
import com.kospot.member.domain.exception.MemberHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true) //todo error handler
public class MemberStatisticAdaptor {

    private final MemberStatisticRepository repository;

    public MemberStatistic queryByMember(Member member) {
        return repository.findByMember(member)
                .orElseThrow(() -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND));
    }

    public MemberStatistic queryByMemberId(Long memberId) {
        return repository.findByMemberId(memberId)
                .orElseThrow(() -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND));
    }

    public MemberStatistic queryByMemberFetchModeStatistics(Member member) {
        return repository.findByMemberFetchModeStatistics(member)
                .orElseThrow(() -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND));
    }

    public MemberStatistic queryByMemberIdFetchModeStatistics(Long memberId) {
        return repository.findByMemberIdFetchModeStatistics(memberId)
                .orElseThrow(() -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND));
    }
}

