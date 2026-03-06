package com.kospot.admin.application.usecase.member;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.domain.vo.Role;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.admin.presentation.dto.response.AdminMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@UseCase
@RequiredArgsConstructor
public class FindAllMembersUseCase {

    private final MemberAdaptor memberAdaptor;

    public Page<AdminMemberResponse.MemberInfo> execute(Long adminId, Pageable pageable, String role) {
        Member admin = memberAdaptor.queryById(adminId);
        admin.validateAdmin();

        if (role != null && !role.isEmpty()) {
            Role memberRole = Role.valueOf(role.toUpperCase());
            return memberAdaptor.queryAllByRoleWithPaging(memberRole, pageable)
                    .map(AdminMemberResponse.MemberInfo::from);
        }

        return memberAdaptor.queryAllWithPaging(pageable)
                .map(AdminMemberResponse.MemberInfo::from);
    }
}

