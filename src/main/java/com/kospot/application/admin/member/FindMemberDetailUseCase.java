package com.kospot.application.admin.member;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.statistic.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.response.AdminMemberResponse;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class FindMemberDetailUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberStatisticAdaptor memberStatisticAdaptor;

    public AdminMemberResponse.MemberDetail execute(Member admin, Long memberId) {
        admin.validateAdmin();

        Member targetMember = memberAdaptor.queryById(memberId);
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(targetMember);

        return AdminMemberResponse.MemberDetail.of(targetMember, statistic);
    }
}

