package com.kospot.domain.member.service;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.exception.MemberHandler;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.member.vo.Role;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Member initializeMember(String username, String email) {
        String nickname = "kospot_" + UUID.randomUUID().toString().substring(0, 8);
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .email(email)
                .firstVisited(true)
                .role(Role.USER)
                .build();
        return memberRepository.save(member);
    }

    private void markVisited(Member member) {
        if(member.isFirstVisited()) {
            member.markVisited();
        }
    }

    public void setNickname(Member member, String nickname) {
        markVisited(member);
        member.setNickname(nickname);
    }

    public void updateNickname(Member member, String nickname) {
        member.setNickname(nickname);
    }

    public void validateAdmin(Member member) {
        if (member.isNotAdmin()) {
            throw new MemberHandler(ErrorStatus.AUTH_ADMIN_PRIVILEGES_REQUIRED);
        }
    }

}
