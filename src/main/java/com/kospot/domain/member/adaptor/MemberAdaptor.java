package com.kospot.domain.member.adaptor;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.exception.object.domain.MemberHandler;
import com.kospot.exception.payload.code.ErrorStatus;
import com.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public List<Member> queryAllByGameRoomId(Long gameRoomId) {
        return repository.findAllByGameRoomId(gameRoomId);
    }

    public Member queryByUsernameFetchEquippedMarkerImage(String username) {
        return repository.findByUsernameFetchEquippedMarkerImage(username).orElseThrow(
                () -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND)
        );
    }
}
