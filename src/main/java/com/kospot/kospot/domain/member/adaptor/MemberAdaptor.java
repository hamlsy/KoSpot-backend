package com.kospot.kospot.domain.member.adaptor;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.member.repository.MemberRepository;
import com.kospot.kospot.exception.object.domain.MemberHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberAdaptor {

    private final MemberRepository repository;

    public Member queryById(Long memberId) {
        return repository.findById(memberId).orElseThrow(
                () -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND)
        );
    }

}
