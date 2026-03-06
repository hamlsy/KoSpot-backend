package com.kospot.admin.application.usecase.member;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.domain.statistic.adaptor.MemberStatisticAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.admin.presentation.dto.response.AdminMemberResponse;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class FindMemberDetailUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberStatisticAdaptor memberStatisticAdaptor;

    public AdminMemberResponse.MemberDetail execute(Long adminId, Long memberId) {
        Member admin = memberAdaptor.queryById(adminId);
        admin.validateAdmin();

        Member targetMember = memberAdaptor.queryById(memberId);
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(targetMember);

        return AdminMemberResponse.MemberDetail.of(targetMember, statistic);
    }
}

