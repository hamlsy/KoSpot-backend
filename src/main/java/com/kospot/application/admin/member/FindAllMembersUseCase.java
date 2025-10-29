package com.kospot.application.admin.member;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.vo.Role;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.response.AdminMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@UseCase
@RequiredArgsConstructor
public class FindAllMembersUseCase {

    private final MemberAdaptor memberAdaptor;

    public Page<AdminMemberResponse.MemberInfo> execute(Member admin, Pageable pageable, String role) {
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

