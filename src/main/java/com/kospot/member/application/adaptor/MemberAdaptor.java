package com.kospot.member.application.adaptor;

import com.kospot.member.domain.entity.Member;
import com.kospot.member.domain.exception.MemberErrorStatus;
import com.kospot.member.infrastructure.persistence.MemberRepository;
import com.kospot.member.domain.vo.Role;
import com.kospot.member.domain.exception.MemberHandler;
import com.kospot.common.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    public int queryPointById(Long memberId) {
        return repository.findPointById(memberId).orElseThrow(
                () -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND)
        );
    }

    public List<Member> queryAllByNicknameKeyword(String keyword) {
        return repository.findAllByNicknameKeyword(keyword);
    }

    public Member queryByIdFetchMarkerImage(Long memberId) {
        return repository.findByIdFetchEquippedMarkerImage(memberId).orElseThrow(
                () -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND)
        );
    }

    public List<Member> queryAllByGameRoomId(Long gameRoomId) {
        return repository.findAllByGameRoomId(gameRoomId);
    }

    public List<Member> queryAllWithGameRoomId() {
        return repository.findAllByGameRoomIdIsNotNull();
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

    public Member queryByEmail(String email) {
        return repository.findByEmail(email).orElseThrow(
                () -> new MemberHandler(MemberErrorStatus.EMAIL_NOT_FOUND)
        );
    }

    public Optional<Member> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    public Member queryFirstBotMember() {
        Member member = repository.findAllBot().get(0);
        if(member == null) {
            throw new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND);
        }
        return member;

    }
}
