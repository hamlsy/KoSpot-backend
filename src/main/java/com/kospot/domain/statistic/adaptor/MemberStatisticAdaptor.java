package com.kospot.domain.statistic.adaptor;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.domain.member.exception.MemberErrorStatus;
import com.kospot.domain.statistic.repository.MemberStatisticRepository;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import com.kospot.domain.member.exception.MemberHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
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


}

