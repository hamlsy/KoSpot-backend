package com.kospot.kospot.domain.member.adaptor;

import com.kospot.kospot.domain.member.entity.Member;

public interface MemberAdaptor {
    Member queryById(Long memberId);
}
