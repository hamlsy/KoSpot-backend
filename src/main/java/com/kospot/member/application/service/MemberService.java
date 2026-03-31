package com.kospot.member.application.service;

import com.kospot.member.domain.entity.Member;
import com.kospot.member.domain.exception.MemberHandler;
import com.kospot.member.infrastructure.persistence.MemberRepository;
import com.kospot.member.domain.vo.AuthProvider;
import com.kospot.member.domain.vo.Role;
import com.kospot.common.exception.payload.code.ErrorStatus;
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

    public Member initializeSocialMember(String username, String email, AuthProvider authProvider) {
        return memberRepository.save(Member.ofSocial(username, email, authProvider));
    }

    public Member initializeLocalMember(String email, String encodedPassword) {
        return memberRepository.save(Member.ofLocal(email, encodedPassword));
    }

    public Member registerAdsenseBot(String username) {
        String nickname = "adsense_bot_" + UUID.randomUUID().toString().substring(0, 8);
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .email("adsense_bot@email")
                .firstVisited(false)
                .role(Role.BOT)
                .authProvider(AuthProvider.LOCAL)
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
