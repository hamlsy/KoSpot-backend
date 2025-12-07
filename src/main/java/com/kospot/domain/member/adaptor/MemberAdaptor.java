package com.kospot.domain.member.adaptor;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.exception.MemberErrorStatus;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.member.exception.MemberHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberAdaptor {

    private final MemberRepository repository;

    public Member queryById(Long memberId) {
        return repository.findById(memberId).orElseThrow(
                () -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND)
        );
    }

    public Member queryByIdFetchMarkerImage(Long memberId) {
        return repository.findByIdFetchEquippedMarkerImage(memberId).orElseThrow(
                () -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND)
        );
    }

    public List<Member> queryAllByGameRoomId(Long gameRoomId) {
        return repository.findAllByGameRoomId(gameRoomId);
    }

    public Member queryByUsernameFetchEquippedMarkerImage(String username) {
        return repository.findByUsernameFetchEquippedMarkerImage(username).orElseThrow(
                () -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND)
        );
    }

    public Member queryByUsername(String username) {
        return repository.findByUsername(username).orElseThrow(
                () -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND)
        );
    }

    public Page<Member> queryAllWithPaging(Pageable pageable) {
        return repository.findAllByOrderByCreatedDateDesc(pageable);
    }

    public Page<Member> queryAllByRoleWithPaging(Role role, Pageable pageable) {
        return repository.findAllByRoleOrderByCreatedDateDesc(role, pageable);
    }

    public List<Member> findAll() {
        return repository.findAll();
    }

    public boolean existsByNickname(String nickname) {
        return repository.existsByNickname(nickname);
    }
}
